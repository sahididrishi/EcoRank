package dev.ecorank.plugin.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.ecorank.plugin.api.BackendClient;
import dev.ecorank.plugin.model.PendingOrder;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orchestrates order fulfillment by applying rank grants/removals for pending orders.
 * Manages a persistent queue of deliveries for offline players.
 *
 * Flow:
 * 1. Receive pending orders from BackendClient
 * 2. For online players: grant/remove rank on main thread, then confirm async
 * 3. For offline players: queue to pendingDeliveries (persisted to JSON file)
 * 4. On player join: apply queued grants/removals
 */
public class FulfillmentService {

    private static final String PENDING_FILE = "pending_deliveries.json";

    private final JavaPlugin plugin;
    private final RankService rankService;
    private final BackendClient backendClient;
    private final Logger logger;
    private final Gson gson;
    private final File pendingFile;

    /**
     * Map of player UUID to list of pending orders for offline players.
     * Thread-safe because it can be accessed from async poll task and main thread join event.
     */
    private final ConcurrentHashMap<UUID, List<PendingOrder>> pendingDeliveries;

    /**
     * Orders that have been fulfilled but not yet confirmed to the backend.
     * Flushed on shutdown.
     */
    private final List<String> pendingConfirmations;

    public FulfillmentService(JavaPlugin plugin, RankService rankService, BackendClient backendClient, Logger logger) {
        this.plugin = plugin;
        this.rankService = rankService;
        this.backendClient = backendClient;
        this.logger = logger;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.pendingFile = new File(plugin.getDataFolder(), PENDING_FILE);
        this.pendingDeliveries = new ConcurrentHashMap<>();
        this.pendingConfirmations = Collections.synchronizedList(new ArrayList<>());

        loadPendingDeliveries();
    }

    /**
     * Processes a list of pending orders from the backend.
     * Called from the async poll task — dispatches to main thread for Bukkit API calls.
     *
     * @param orders the pending orders to process
     */
    public void processOrders(List<PendingOrder> orders) {
        if (orders.isEmpty()) return;

        logger.info("Processing " + orders.size() + " pending order(s).");

        // Dispatch to main thread — getPlayer() and other Bukkit API calls must be on main thread
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (PendingOrder order : orders) {
                // Skip orders with in-flight confirmations (prevents duplicate delivery)
                if (pendingConfirmations.contains(order.orderId())) {
                    continue;
                }

                var player = plugin.getServer().getPlayer(order.playerUuid());

                if (player != null && player.isOnline()) {
                    applyOrder(order);
                } else {
                    queueForOfflinePlayer(order);
                }
            }
        });
    }

    /**
     * Called on the main thread when a player joins.
     * Applies any queued deliveries/removals.
     *
     * @param playerUuid the joining player's UUID
     */
    public void processPlayerJoin(UUID playerUuid) {
        List<PendingOrder> queued = pendingDeliveries.remove(playerUuid);
        if (queued == null || queued.isEmpty()) return;

        logger.info("Applying " + queued.size() + " queued order(s) for " + playerUuid);
        for (PendingOrder order : queued) {
            applyOrder(order);
        }

        savePendingDeliveries();
    }

    /**
     * Checks if a player has pending deliveries.
     */
    public boolean hasPendingDeliveries(UUID playerUuid) {
        List<PendingOrder> queued = pendingDeliveries.get(playerUuid);
        return queued != null && !queued.isEmpty();
    }

    /**
     * Applies a single order: grants or removes rank, then confirms to backend asynchronously.
     * MUST be called on the main server thread.
     */
    private void applyOrder(PendingOrder order) {
        if (order.isGrant()) {
            rankService.grantRank(order.playerUuid(), order.rankGroup())
                    .thenAccept(success -> {
                        if (success) {
                            confirmOrderAsync(order.orderId());
                            logger.info("Granted rank '" + order.rankGroup() + "' for order " + order.orderId());
                        } else {
                            logger.warning("Failed to grant rank for order " + order.orderId() + ", will retry");
                            // Dispatch back to main thread to safely modify pendingDeliveries
                            plugin.getServer().getScheduler().runTask(plugin, () -> queueForOfflinePlayer(order));
                        }
                    });
        } else if (order.isRemoval()) {
            rankService.removeRank(order.playerUuid(), order.rankGroup())
                    .thenAccept(success -> {
                        if (success) {
                            confirmOrderAsync(order.orderId());
                            logger.info("Removed rank '" + order.rankGroup() + "' for order " + order.orderId());
                        } else {
                            logger.warning("Failed to remove rank for order " + order.orderId() + ", will retry");
                            plugin.getServer().getScheduler().runTask(plugin, () -> queueForOfflinePlayer(order));
                        }
                    });
        } else {
            logger.warning("Unknown order action: " + order.action() + " for order " + order.orderId());
        }
    }

    /**
     * Confirms fulfillment to the backend asynchronously (off the main thread).
     */
    private void confirmOrderAsync(String orderId) {
        pendingConfirmations.add(orderId);

        // Run confirmation on an async thread — never block the main thread for HTTP
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            backendClient.confirmFulfillment(orderId).thenAccept(success -> {
                if (success) {
                    pendingConfirmations.remove(orderId);
                } else {
                    logger.warning("Failed to confirm order " + orderId + " to backend, will retry on shutdown");
                }
            });
        });
    }

    /**
     * Queues an order for an offline player. Persists to disk.
     */
    private void queueForOfflinePlayer(PendingOrder order) {
        pendingDeliveries.computeIfAbsent(order.playerUuid(), k -> Collections.synchronizedList(new ArrayList<>()))
                .add(order);
        savePendingDeliveries();
        logger.info("Queued order " + order.orderId() + " for offline player " + order.playerUuid());
    }

    /**
     * Loads pending deliveries from the JSON file on disk.
     */
    private void loadPendingDeliveries() {
        if (!pendingFile.exists()) return;

        try (Reader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(pendingFile), StandardCharsets.UTF_8))) {
            Type type = new TypeToken<Map<String, List<PendingOrderDto>>>() {}.getType();
            Map<String, List<PendingOrderDto>> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                loaded.forEach((uuidStr, dtoList) -> {
                    UUID uuid = UUID.fromString(uuidStr);
                    List<PendingOrder> orders = new ArrayList<>();
                    for (PendingOrderDto dto : dtoList) {
                        orders.add(new PendingOrder(
                                dto.orderId, UUID.fromString(dto.playerUuid),
                                dto.productSlug, dto.rankGroup, dto.action, dto.amountCents));
                    }
                    pendingDeliveries.put(uuid, Collections.synchronizedList(orders));
                });
            }
            logger.info("Loaded " + pendingDeliveries.size() + " pending delivery queue(s) from disk.");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to load pending deliveries from " + PENDING_FILE, e);
        }
    }

    /**
     * Saves pending deliveries to the JSON file on disk.
     */
    private void savePendingDeliveries() {
        try {
            if (!pendingFile.getParentFile().exists()) {
                pendingFile.getParentFile().mkdirs();
            }

            Map<String, List<PendingOrderDto>> toSave = new LinkedHashMap<>();
            pendingDeliveries.forEach((uuid, orders) -> {
                List<PendingOrderDto> dtoList = new ArrayList<>();
                synchronized (orders) {
                    for (PendingOrder order : orders) {
                        dtoList.add(new PendingOrderDto(
                                order.orderId(), order.playerUuid().toString(),
                                order.productSlug(), order.rankGroup(), order.action(), order.amountCents()));
                    }
                }
                toSave.put(uuid.toString(), dtoList);
            });

            try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(pendingFile), StandardCharsets.UTF_8))) {
                gson.toJson(toSave, writer);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to save pending deliveries to " + PENDING_FILE, e);
        }
    }

    /**
     * Shutdown: flush any pending confirmations synchronously.
     */
    public void shutdown() {
        logger.info("Flushing " + pendingConfirmations.size() + " pending confirmation(s)...");
        List<String> toConfirm = new ArrayList<>(pendingConfirmations);
        for (String orderId : toConfirm) {
            try {
                boolean result = backendClient.confirmFulfillment(orderId).join();
                if (result) {
                    pendingConfirmations.remove(orderId);
                    logger.info("Flushed confirmation for order " + orderId);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to flush confirmation for order " + orderId, e);
            }
        }

        savePendingDeliveries();
        logger.info("FulfillmentService shut down. " +
                pendingDeliveries.size() + " offline queue(s), " +
                pendingConfirmations.size() + " unconfirmed order(s).");
    }

    /**
     * Simple DTO for JSON serialization of pending orders (records don't serialize cleanly with Gson by default).
     */
    private static class PendingOrderDto {
        String orderId;
        String playerUuid;
        String productSlug;
        String rankGroup;
        String action;
        int amountCents;

        PendingOrderDto(String orderId, String playerUuid, String productSlug,
                        String rankGroup, String action, int amountCents) {
            this.orderId = orderId;
            this.playerUuid = playerUuid;
            this.productSlug = productSlug;
            this.rankGroup = rankGroup;
            this.action = action;
            this.amountCents = amountCents;
        }
    }
}
