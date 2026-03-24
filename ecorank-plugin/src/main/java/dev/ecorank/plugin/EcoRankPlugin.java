package dev.ecorank.plugin;

import dev.ecorank.plugin.api.BackendClient;
import dev.ecorank.plugin.api.EcoRankPlaceholderExpansion;
import dev.ecorank.plugin.command.BalTopCommand;
import dev.ecorank.plugin.command.BalanceCommand;
import dev.ecorank.plugin.command.EcoCommand;
import dev.ecorank.plugin.command.PayCommand;
import dev.ecorank.plugin.config.ConfigService;
import dev.ecorank.plugin.listener.ChestQuestListener;
import dev.ecorank.plugin.listener.MobKillListener;
import dev.ecorank.plugin.listener.PlayerJoinListener;
import dev.ecorank.plugin.service.EconomyService;
import dev.ecorank.plugin.service.FulfillmentService;
import dev.ecorank.plugin.service.RankService;
import dev.ecorank.plugin.storage.MysqlStorageProvider;
import dev.ecorank.plugin.storage.SqliteStorageProvider;
import dev.ecorank.plugin.storage.StorageProvider;
import dev.ecorank.plugin.task.FulfillmentPollTask;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

/**
 * EcoRank — Economy, donation store, and rank management plugin.
 *
 * This is a thin entry point that wires together all services.
 * Business logic lives in the service/, storage/, and listener/ packages.
 */
@SuppressWarnings("UnstableApiUsage")
public class EcoRankPlugin extends JavaPlugin {

    private ConfigService configService;
    private StorageProvider storageProvider;
    private EconomyService economyService;
    private RankService rankService;
    private BackendClient backendClient;
    private FulfillmentService fulfillmentService;
    private FulfillmentPollTask pollTask;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        // Save default config if not present
        saveDefaultConfig();

        // Initialize ConfigService (fail-fast on missing required values)
        try {
            configService = new ConfigService(getConfig());
        } catch (IllegalStateException e) {
            getLogger().severe("Configuration error: " + e.getMessage());
            getLogger().severe("EcoRank will now disable. Fix config.yml and restart.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize storage
        try {
            storageProvider = createStorageProvider();
            storageProvider.initialize();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize storage", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize services
        economyService = new EconomyService(storageProvider, configService, getLogger());

        rankService = new RankService(getLogger());
        try {
            rankService.initialize();
        } catch (IllegalStateException e) {
            getLogger().log(Level.SEVERE, "Failed to connect to LuckPerms", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        backendClient = new BackendClient(configService, getLogger());
        fulfillmentService = new FulfillmentService(this, rankService, backendClient, getLogger());

        // Register event listeners
        registerListeners();

        // Register commands via LifecycleEventManager + Brigadier
        registerCommands();

        // Register PlaceholderAPI expansion if present
        registerPlaceholders();

        // Start fulfillment poll task
        pollTask = new FulfillmentPollTask(
                this, backendClient, fulfillmentService, getLogger(),
                configService.getPollIntervalSeconds(),
                configService.getMaxBackoffSeconds());
        pollTask.start();

        long elapsed = System.currentTimeMillis() - startTime;
        getLogger().info("EcoRank enabled in " + elapsed + "ms. Storage: " +
                configService.getStorageType() + ", Server-ID: " + configService.getServerId());
    }

    @Override
    public void onDisable() {
        // Cancel poll task
        if (pollTask != null) {
            pollTask.stop();
            getLogger().info("Fulfillment poll task stopped.");
        }

        // Flush pending fulfillment confirmations
        if (fulfillmentService != null) {
            fulfillmentService.shutdown();
        }

        // Shutdown backend client
        if (backendClient != null) {
            backendClient.shutdown();
        }

        // Close storage connections
        if (storageProvider != null) {
            storageProvider.shutdown();
        }

        getLogger().info("EcoRank disabled.");
    }

    private StorageProvider createStorageProvider() {
        String type = configService.getStorageType().toLowerCase();
        return switch (type) {
            case "mysql" -> new MysqlStorageProvider(getLogger(), configService);
            case "sqlite" -> new SqliteStorageProvider(getLogger(),
                    new File(getDataFolder(), configService.getSqliteFile()));
            default -> {
                getLogger().warning("Unknown storage type '" + type + "', falling back to SQLite.");
                yield new SqliteStorageProvider(getLogger(),
                        new File(getDataFolder(), configService.getSqliteFile()));
            }
        };
    }

    private void registerListeners() {
        var pluginManager = getServer().getPluginManager();

        pluginManager.registerEvents(
                new PlayerJoinListener(economyService, configService, storageProvider,
                        fulfillmentService, getLogger()),
                this);

        pluginManager.registerEvents(
                new MobKillListener(economyService, configService, getLogger()),
                this);

        pluginManager.registerEvents(
                new ChestQuestListener(economyService, configService, getLogger()),
                this);

        getLogger().info("Event listeners registered.");
    }

    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            var registrar = event.registrar();

            registrar.register(
                    BalanceCommand.createCommand(economyService, configService),
                    "Check your balance or another player's balance.",
                    List.of("bal", "money"));

            registrar.register(
                    PayCommand.createCommand(economyService, configService),
                    "Pay another player.");

            registrar.register(
                    BalTopCommand.createCommand(economyService, configService),
                    "View the balance leaderboard.",
                    List.of("balancetop"));

            registrar.register(
                    EcoCommand.createCommand(economyService, configService),
                    "Admin economy management.");
        });

        getLogger().info("Commands registered via LifecycleEventManager.");
    }

    private void registerPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new EcoRankPlaceholderExpansion(economyService, configService, rankService).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        } else {
            getLogger().info("PlaceholderAPI not found, placeholders will not be available.");
        }
    }

    // --- Accessors for other components (e.g., API consumers) ---

    public ConfigService getConfigService() { return configService; }
    public EconomyService getEconomyService() { return economyService; }
    public RankService getRankService() { return rankService; }
    public FulfillmentService getFulfillmentService() { return fulfillmentService; }
}
