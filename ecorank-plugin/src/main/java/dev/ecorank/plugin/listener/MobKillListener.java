package dev.ecorank.plugin.listener;

import dev.ecorank.plugin.config.ConfigService;
import dev.ecorank.plugin.model.TransactionType;
import dev.ecorank.plugin.service.EconomyService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.logging.Logger;

/**
 * Awards configurable coins when a player kills a mob.
 * Uses per-type rewards from config with a default fallback.
 * Displays an action bar message showing the reward.
 */
public class MobKillListener implements Listener {

    private final EconomyService economyService;
    private final ConfigService configService;
    private final Logger logger;

    public MobKillListener(EconomyService economyService, ConfigService configService, Logger logger) {
        this.economyService = economyService;
        this.configService = configService;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!configService.isMobKillEnabled()) return;

        // Only reward player kills, not other causes of death
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        // Don't reward killing players
        EntityType entityType = event.getEntityType();
        if (entityType == EntityType.PLAYER) return;

        long reward = configService.getMobKillReward(entityType);
        if (reward <= 0) return;

        String mobName = formatEntityName(entityType);
        boolean success = economyService.deposit(
                killer.getUniqueId(), reward,
                TransactionType.MOB_KILL, "Killed " + mobName);

        if (success) {
            // Show action bar message (non-intrusive)
            killer.sendActionBar(Component.text()
                    .append(Component.text("+" + configService.formatAmount(reward), NamedTextColor.GREEN))
                    .append(Component.text(" (" + mobName + ")", NamedTextColor.GRAY))
                    .build());
        }
    }

    /**
     * Converts an EntityType enum name to a display-friendly format.
     * e.g., WITHER_SKELETON -> Wither Skeleton
     */
    private static String formatEntityName(EntityType type) {
        String[] parts = type.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(Character.toUpperCase(parts[i].charAt(0)));
            sb.append(parts[i].substring(1));
        }
        return sb.toString();
    }
}
