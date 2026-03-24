package dev.ecorank.plugin.api;

import dev.ecorank.plugin.config.ConfigService;
import dev.ecorank.plugin.service.EconomyService;
import dev.ecorank.plugin.service.RankService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion for EcoRank.
 * Provides placeholders:
 *   %ecorank_balance%     — formatted balance (e.g., "$150")
 *   %ecorank_balance_raw% — raw integer balance (e.g., "150")
 *   %ecorank_rank%        — player's primary LuckPerms group
 *   %ecorank_currency%    — currency name
 */
public class EcoRankPlaceholderExpansion extends PlaceholderExpansion {

    private final EconomyService economyService;
    private final ConfigService configService;
    private final RankService rankService;

    public EcoRankPlaceholderExpansion(EconomyService economyService, ConfigService configService,
                                       RankService rankService) {
        this.economyService = economyService;
        this.configService = configService;
        this.rankService = rankService;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ecorank";
    }

    @Override
    public @NotNull String getAuthor() {
        return "EcoRank";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        // Survive PlaceholderAPI reloads
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return null;

        return switch (params.toLowerCase()) {
            case "balance" -> {
                long balance = economyService.getBalance(player.getUniqueId());
                yield configService.formatAmount(balance);
            }
            case "balance_raw" -> {
                long balance = economyService.getBalance(player.getUniqueId());
                yield String.valueOf(balance);
            }
            case "rank" -> rankService.getPrimaryGroup(player.getUniqueId());
            case "currency" -> configService.getCurrencyName();
            default -> null;
        };
    }
}
