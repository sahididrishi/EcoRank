package dev.ecorank.plugin.listener;

import dev.ecorank.plugin.config.ConfigService;
import dev.ecorank.plugin.model.TransactionType;
import dev.ecorank.plugin.service.EconomyService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Awards coins when a player opens a chest block, with per-player cooldown tracking.
 * Cooldown is in-memory only (resets on server restart).
 */
public class ChestQuestListener implements Listener {

    private final EconomyService economyService;
    private final ConfigService configService;
    private final Logger logger;

    /**
     * In-memory cooldown tracking: player UUID -> last chest quest reward time.
     */
    private final Map<UUID, Instant> cooldowns = new ConcurrentHashMap<>();

    public ChestQuestListener(EconomyService economyService, ConfigService configService, Logger logger) {
        this.economyService = economyService;
        this.configService = configService;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!configService.isChestQuestEnabled()) return;

        // Only right-click block actions
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        // Only chest-type blocks
        Material type = block.getType();
        if (type != Material.CHEST && type != Material.TRAPPED_CHEST
                && type != Material.ENDER_CHEST && type != Material.BARREL) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Check cooldown
        Instant lastReward = cooldowns.get(playerId);
        if (lastReward != null) {
            Duration elapsed = Duration.between(lastReward, Instant.now());
            int cooldownSeconds = configService.getChestQuestCooldownSeconds();
            if (elapsed.getSeconds() < cooldownSeconds) {
                long remaining = cooldownSeconds - elapsed.getSeconds();
                player.sendActionBar(Component.text()
                        .append(Component.text("Chest Quest on cooldown: ", NamedTextColor.GRAY))
                        .append(Component.text(remaining + "s", NamedTextColor.RED))
                        .build());
                return;
            }
        }

        long reward = configService.getChestQuestAmount();
        if (reward <= 0) return;

        boolean success = economyService.deposit(
                playerId, reward,
                TransactionType.CHEST_QUEST, "Chest quest reward");

        if (success) {
            cooldowns.put(playerId, Instant.now());
            player.sendMessage(Component.text()
                    .append(Component.text("[EcoRank] ", NamedTextColor.GOLD))
                    .append(Component.text("Chest Quest: ", NamedTextColor.GREEN))
                    .append(Component.text("+" + configService.formatAmount(reward), NamedTextColor.YELLOW))
                    .build());
        }
    }

    /**
     * Removes a player's cooldown entry (called on quit to avoid memory leaks in long sessions).
     */
    public void clearCooldown(UUID playerId) {
        cooldowns.remove(playerId);
    }
}
