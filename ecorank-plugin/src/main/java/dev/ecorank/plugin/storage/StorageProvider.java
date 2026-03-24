package dev.ecorank.plugin.storage;

import dev.ecorank.plugin.model.EconomyTransaction;
import dev.ecorank.plugin.model.PlayerAccount;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface for all economy data persistence operations.
 * Implementations handle connection pooling and thread safety.
 */
public interface StorageProvider {

    /**
     * Initializes the storage backend — creates tables, establishes connections.
     * Called once during plugin enable.
     *
     * @throws Exception if initialization fails (plugin should disable itself)
     */
    void initialize() throws Exception;

    /**
     * Shuts down the storage backend — closes connection pools.
     * Called once during plugin disable.
     */
    void shutdown();

    /**
     * Gets an existing account or creates a new one with the given starting balance.
     *
     * @param playerId        the player's UUID
     * @param playerName      the player's current username
     * @param startingBalance the balance for new accounts
     * @return the player's account (created or existing)
     */
    PlayerAccount getOrCreateAccount(UUID playerId, String playerName, long startingBalance);

    /**
     * Gets an existing account.
     *
     * @param playerId the player's UUID
     * @return the account if it exists, empty otherwise
     */
    Optional<PlayerAccount> getAccount(UUID playerId);

    /**
     * Updates a player's balance using optimistic concurrency control.
     * The update only succeeds if the current balance matches expectedBalance.
     *
     * @param playerId        the player's UUID
     * @param newBalance      the desired new balance
     * @param expectedBalance the expected current balance (for optimistic locking)
     * @return true if the update succeeded, false if the balance was modified concurrently
     */
    boolean updateBalance(UUID playerId, long newBalance, long expectedBalance);

    /**
     * Logs an economy transaction for audit purposes.
     *
     * @param transaction the transaction to record
     */
    void logTransaction(EconomyTransaction transaction);

    /**
     * Gets the top player balances for the leaderboard.
     *
     * @param limit maximum number of results
     * @return list of accounts sorted by balance descending
     */
    List<PlayerAccount> getTopBalances(int limit);

    /**
     * Updates the player's last daily reward timestamp to now.
     *
     * @param playerId the player's UUID
     */
    void updateLastDailyReward(UUID playerId);

    /**
     * Updates the player's last login timestamp to now and their username.
     *
     * @param playerId   the player's UUID
     * @param playerName the player's current username
     */
    void updateLastLogin(UUID playerId, String playerName);
}
