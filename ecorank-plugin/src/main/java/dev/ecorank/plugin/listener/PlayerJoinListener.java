package dev.ecorank.plugin.listener;

import dev.ecorank.plugin.config.ConfigService;
import dev.ecorank.plugin.model.PlayerAccount;
import dev.ecorank.plugin.model.TransactionType;
import dev.ecorank.plugin.service.EconomyService;
import dev.ecorank.plugin.service.FulfillmentService;
import dev.ecorank.plugin.storage.StorageProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;

/**
 * Handles player join events:
 * 1. Ensures economy account exists
 * 2. Updates last login timestamp
 * 3. Checks and awards daily login reward if eligible
 * 4. Processes any pending deliveries/removals via FulfillmentService
 */
public class PlayerJoinListener implements Listener {

    private final EconomyService economyService;
    private final ConfigService configService;
    private final StorageProvider storageProvider;
    private final FulfillmentService fulfillmentService;
    private final Logger logger;

    public PlayerJoinListener(EconomyService economyService, ConfigService configService,
                              StorageProvider storageProvider, FulfillmentService fulfillmentService,
                              Logger logger) {
        this.economyService = economyService;
        this.configService = configService;
        this.storageProvider = storageProvider;
        this.fulfillmentService = fulfillmentService;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Ensure account exists (creates with starting balance if new)
        PlayerAccount account = economyService.ensureAccount(
                player.getUniqueId(), player.getName());

        // Update last login
        storageProvider.updateLastLogin(player.getUniqueId(), player.getName());

        // Daily login reward
        if (configService.isDailyLoginEnabled()) {
            checkDailyReward(player, account);
        }

        // Process pending fulfillment deliveries/removals
        if (fulfillmentService.hasPendingDeliveries(player.getUniqueId())) {
            fulfillmentService.processPlayerJoin(player.getUniqueId());
        }
    }

    private void checkDailyReward(Player player, PlayerAccount account) {
        Instant lastReward = account.lastDailyReward();
        int cooldownHours = configService.getDailyLoginCooldownHours();

        boolean eligible;
        if (lastReward == null) {
            // Never claimed — eligible
            eligible = true;
        } else {
            Duration elapsed = Duration.between(lastReward, Instant.now());
            eligible = elapsed.toHours() >= cooldownHours;
        }

        if (eligible) {
            long rewardAmount = configService.getDailyLoginAmount();
            boolean deposited = economyService.deposit(
                    player.getUniqueId(), rewardAmount,
                    TransactionType.DAILY_LOGIN, "Daily login reward");

            if (deposited) {
                economyService.updateLastDailyReward(player.getUniqueId());
                player.sendMessage(Component.text()
                        .append(Component.text("[EcoRank] ", NamedTextColor.GOLD))
                        .append(Component.text("Daily login reward: ", NamedTextColor.GREEN))
                        .append(Component.text("+" + configService.formatAmount(rewardAmount), NamedTextColor.YELLOW))
                        .build());
                logger.fine("Awarded daily login reward of " + rewardAmount + " to " + player.getName());
            }
        }
    }
}
