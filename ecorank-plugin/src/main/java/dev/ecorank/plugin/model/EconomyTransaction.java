package dev.ecorank.plugin.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable record of an economy transaction.
 *
 * @param id           auto-generated database ID (0 for unsaved transactions)
 * @param playerId     the UUID of the player involved
 * @param amount       signed amount: positive for deposits, negative for withdrawals
 * @param balanceAfter the player's balance after this transaction
 * @param type         the category of this transaction
 * @param description  human-readable description of what caused this transaction
 * @param timestamp    when this transaction occurred
 */
public record EconomyTransaction(
        long id,
        UUID playerId,
        long amount,
        long balanceAfter,
        TransactionType type,
        String description,
        Instant timestamp
) {
    public EconomyTransaction {
        if (playerId == null) throw new IllegalArgumentException("playerId must not be null");
        if (type == null) throw new IllegalArgumentException("type must not be null");
        if (balanceAfter < 0) throw new IllegalArgumentException("balanceAfter must not be negative");
        if (timestamp == null) throw new IllegalArgumentException("timestamp must not be null");
        if (description == null) description = "";
    }

    /**
     * Factory for creating a new (unsaved) transaction with the current timestamp.
     */
    public static EconomyTransaction create(UUID playerId, long amount, long balanceAfter,
                                             TransactionType type, String description) {
        return new EconomyTransaction(0, playerId, amount, balanceAfter, type, description, Instant.now());
    }
}
