package dev.ecorank.plugin.task;

import dev.ecorank.plugin.api.BackendClient;
import dev.ecorank.plugin.model.PendingOrder;
import dev.ecorank.plugin.service.FulfillmentService;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Self-scheduling BukkitRunnable that polls the backend for pending orders.
 *
 * Uses exponential backoff: starts at the configured poll interval (default 5s),
 * doubles up to max-backoff-seconds (default 30s) when no orders are found.
 * Resets to the base interval when orders are found.
 *
 * The HTTP call runs asynchronously, but order processing dispatches to the main
 * thread for Bukkit API calls.
 */
public class FulfillmentPollTask extends BukkitRunnable {

    private final JavaPlugin plugin;
    private final BackendClient backendClient;
    private final FulfillmentService fulfillmentService;
    private final Logger logger;
    private final long baseIntervalTicks;
    private final long maxBackoffTicks;

    private long currentIntervalTicks;
    private boolean running = true;

    /**
     * @param plugin             the owning plugin instance
     * @param backendClient      for fetching pending orders
     * @param fulfillmentService for processing fetched orders
     * @param logger             for logging poll activity
     * @param pollIntervalSeconds base poll interval in seconds
     * @param maxBackoffSeconds   maximum backoff interval in seconds
     */
    public FulfillmentPollTask(JavaPlugin plugin, BackendClient backendClient,
                               FulfillmentService fulfillmentService, Logger logger,
                               int pollIntervalSeconds, int maxBackoffSeconds) {
        this.plugin = plugin;
        this.backendClient = backendClient;
        this.fulfillmentService = fulfillmentService;
        this.logger = logger;
        this.baseIntervalTicks = pollIntervalSeconds * 20L; // 20 ticks per second
        this.maxBackoffTicks = maxBackoffSeconds * 20L;
        this.currentIntervalTicks = baseIntervalTicks;
    }

    /**
     * Starts the poll task running asynchronously.
     */
    public void start() {
        // First poll after the base interval, then self-schedules
        this.runTaskLaterAsynchronously(plugin, baseIntervalTicks);
    }

    @Override
    public void run() {
        if (!running) return;

        try {
            backendClient.fetchPendingOrders().thenAccept(this::handleOrders);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during fulfillment poll", e);
        }

        // Self-schedule the next poll with current interval
        if (running) {
            scheduleNext();
        }
    }

    private void handleOrders(List<PendingOrder> orders) {
        if (orders.isEmpty()) {
            // No activity — increase backoff
            currentIntervalTicks = Math.min(currentIntervalTicks * 2, maxBackoffTicks);
            logger.fine("No pending orders, backoff to " + (currentIntervalTicks / 20) + "s");
        } else {
            // Activity detected — reset to base interval
            currentIntervalTicks = baseIntervalTicks;
            logger.info("Found " + orders.size() + " pending order(s), resetting poll interval");

            // Process on the main thread (FulfillmentService handles the main-thread dispatch internally)
            fulfillmentService.processOrders(orders);
        }
    }

    private void scheduleNext() {
        try {
            new FulfillmentPollTaskContinuation(this)
                    .runTaskLaterAsynchronously(plugin, currentIntervalTicks);
        } catch (IllegalStateException e) {
            // Plugin may be disabling — ignore
            logger.fine("Could not schedule next poll, plugin may be shutting down");
        }
    }

    /**
     * Stops the poll task.
     */
    public void stop() {
        running = false;
        try {
            cancel();
        } catch (IllegalStateException e) {
            // Already cancelled or never scheduled
        }
    }

    /**
     * Inner runnable for continuation scheduling (avoids re-using a cancelled BukkitRunnable).
     */
    private static class FulfillmentPollTaskContinuation extends BukkitRunnable {
        private final FulfillmentPollTask parent;

        FulfillmentPollTaskContinuation(FulfillmentPollTask parent) {
            this.parent = parent;
        }

        @Override
        public void run() {
            parent.run();
        }
    }
}
