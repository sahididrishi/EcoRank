package dev.ecorank.plugin.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a player's economy account.
 *
 * @param playerId       the player's unique Minecraft UUID
 * @param playerName     the player's last known username
 * @param balance        current balance in integer coins (never negative)
 * @param lastLogin      timestamp of the player's most recent login
 * @param lastDailyReward timestamp of the last daily reward claim, or null if never claimed
 */
public record PlayerAccount(
        UUID playerId,
        String playerName,
        long balance,
        Instant lastLogin,
        Instant lastDailyReward
) {
    public PlayerAccount {
        if (playerId == null) throw new IllegalArgumentException("playerId must not be null");
        if (playerName == null || playerName.isBlank()) throw new IllegalArgumentException("playerName must not be blank");
        if (balance < 0) throw new IllegalArgumentException("balance must not be negative");
    }

    /**
     * Returns a copy with the specified balance.
     */
    public PlayerAccount withBalance(long newBalance) {
        return new PlayerAccount(playerId, playerName, newBalance, lastLogin, lastDailyReward);
    }

    /**
     * Returns a copy with the last daily reward updated to now.
     */
    public PlayerAccount withDailyRewardClaimed() {
        return new PlayerAccount(playerId, playerName, balance, lastLogin, Instant.now());
    }

    /**
     * Returns a copy with the last login updated to now.
     */
    public PlayerAccount withLoginUpdated() {
        return new PlayerAccount(playerId, playerName, balance, Instant.now(), lastDailyReward);
    }
}
