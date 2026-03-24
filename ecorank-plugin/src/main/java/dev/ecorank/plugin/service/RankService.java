package dev.ecorank.plugin.service;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * LuckPerms integration for granting and removing rank groups.
 * All operations return CompletableFutures as they involve async I/O via LuckPerms.
 */
public class RankService {

    private final Logger logger;
    private LuckPerms luckPerms;

    public RankService(Logger logger) {
        this.logger = logger;
    }

    /**
     * Initializes the LuckPerms API connection via RegisteredServiceProvider.
     *
     * @throws IllegalStateException if LuckPerms is not available
     */
    public void initialize() {
        try {
            this.luckPerms = LuckPermsProvider.get();
            logger.info("LuckPerms API connected successfully.");
        } catch (IllegalStateException e) {
            throw new IllegalStateException("LuckPerms is not loaded. EcoRank requires LuckPerms to function.", e);
        }
    }

    /**
     * Grants a rank group to a player.
     * Uses LuckPerms modifyUser which handles both online and offline players.
     *
     * @param playerUuid the player's UUID
     * @param groupName  the LuckPerms group name to grant
     * @return a future that resolves to true if the grant succeeded
     */
    public CompletableFuture<Boolean> grantRank(UUID playerUuid, String groupName) {
        InheritanceNode node = InheritanceNode.builder(groupName).build();

        return luckPerms.getUserManager().modifyUser(playerUuid, user -> {
            user.data().add(node);
        }).thenApply(v -> {
            logger.info("Granted rank '" + groupName + "' to " + playerUuid);
            return true;
        }).exceptionally(throwable -> {
            logger.log(Level.SEVERE, "Failed to grant rank '" + groupName + "' to " + playerUuid, throwable);
            return false;
        });
    }

    /**
     * Removes a rank group from a player (e.g., refund-triggered removal).
     * Uses LuckPerms modifyUser which handles both online and offline players.
     *
     * @param playerUuid the player's UUID
     * @param groupName  the LuckPerms group name to remove
     * @return a future that resolves to true if the removal succeeded
     */
    public CompletableFuture<Boolean> removeRank(UUID playerUuid, String groupName) {
        InheritanceNode node = InheritanceNode.builder(groupName).build();

        return luckPerms.getUserManager().modifyUser(playerUuid, user -> {
            user.data().remove(node);
        }).thenApply(v -> {
            logger.info("Removed rank '" + groupName + "' from " + playerUuid);
            return true;
        }).exceptionally(throwable -> {
            logger.log(Level.SEVERE, "Failed to remove rank '" + groupName + "' from " + playerUuid, throwable);
            return false;
        });
    }

    /**
     * Gets the primary group for a player.
     *
     * @param playerUuid the player's UUID
     * @return the primary group name, or "default" if not found
     */
    public String getPrimaryGroup(UUID playerUuid) {
        User user = luckPerms.getUserManager().getUser(playerUuid);
        if (user != null) {
            return user.getPrimaryGroup();
        }
        return "default";
    }

    /**
     * Gets the primary group for a player, loading from storage if needed.
     *
     * @param playerUuid the player's UUID
     * @return a future that resolves to the primary group name
     */
    public CompletableFuture<String> getPrimaryGroupAsync(UUID playerUuid) {
        return luckPerms.getUserManager().loadUser(playerUuid)
                .thenApply(User::getPrimaryGroup)
                .exceptionally(throwable -> {
                    logger.log(Level.WARNING, "Failed to load primary group for " + playerUuid, throwable);
                    return "default";
                });
    }
}
