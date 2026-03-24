package dev.ecorank.plugin.service;

import dev.ecorank.plugin.config.ConfigService;
import dev.ecorank.plugin.model.EconomyTransaction;
import dev.ecorank.plugin.model.PlayerAccount;
import dev.ecorank.plugin.model.TransactionType;
import dev.ecorank.plugin.storage.StorageProvider;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Core economy service handling balance CRUD, transfers, and transaction logging.
 * Uses optimistic concurrency with retry for safe concurrent balance updates.
 */
public class EconomyService {

    private static final int MAX_RETRIES = 3;

    private final StorageProvider storage;
    private final ConfigService configService;
    private final Logger logger;

    public EconomyService(StorageProvider storage, ConfigService configService, Logger logger) {
        this.storage = storage;
        this.configService = configService;
        this.logger = logger;
    }

    /**
     * Ensures a player account exists, creating one with the configured starting balance if needed.
     *
     * @param playerId   the player's UUID
     * @param playerName the player's current username
     * @return the player's account
     */
    public PlayerAccount ensureAccount(UUID playerId, String playerName) {
        return storage.getOrCreateAccount(playerId, playerName, configService.getStartingBalance());
    }

    /**
     * Gets the balance for a player. Returns 0 if the account doesn't exist.
     *
     * @param playerId the player's UUID
     * @return the player's current balance
     */
    public long getBalance(UUID playerId) {
        return storage.getAccount(playerId)
                .map(PlayerAccount::balance)
                .orElse(0L);
    }

    /**
     * Gets the player account if it exists.
     *
     * @param playerId the player's UUID
     * @return the player's account, or empty if not found
     */
    public Optional<PlayerAccount> getAccount(UUID playerId) {
        return storage.getAccount(playerId);
    }

    /**
     * Deposits coins into a player's account.
     *
     * @param playerId    the player's UUID
     * @param amount      the amount to deposit (must be positive)
     * @param type        the transaction type
     * @param description a human-readable reason
     * @return true if the deposit succeeded
     * @throws IllegalArgumentException if amount is not positive
     */
    public boolean deposit(UUID playerId, long amount, TransactionType type, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive, got: " + amount);
        }

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            Optional<PlayerAccount> accountOpt = storage.getAccount(playerId);
            if (accountOpt.isEmpty()) {
                logger.warning("Cannot deposit to non-existent account: " + playerId);
                return false;
            }

            PlayerAccount account = accountOpt.get();
            long currentBalance = account.balance();
            long newBalance = currentBalance + amount;

            if (storage.updateBalance(playerId, newBalance, currentBalance)) {
                storage.logTransaction(EconomyTransaction.create(
                        playerId, amount, newBalance, type, description));
                return true;
            }
            // Optimistic lock failed, retry
            logger.fine("Optimistic lock failed for deposit to " + playerId + ", attempt " + (attempt + 1));
        }

        logger.warning("Failed to deposit to " + playerId + " after " + MAX_RETRIES + " retries");
        return false;
    }

    /**
     * Withdraws coins from a player's account.
     *
     * @param playerId    the player's UUID
     * @param amount      the amount to withdraw (must be positive)
     * @param type        the transaction type
     * @param description a human-readable reason
     * @return true if the withdrawal succeeded, false if insufficient funds or concurrency failure
     * @throws IllegalArgumentException if amount is not positive
     */
    public boolean withdraw(UUID playerId, long amount, TransactionType type, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdraw amount must be positive, got: " + amount);
        }

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            Optional<PlayerAccount> accountOpt = storage.getAccount(playerId);
            if (accountOpt.isEmpty()) {
                logger.warning("Cannot withdraw from non-existent account: " + playerId);
                return false;
            }

            PlayerAccount account = accountOpt.get();
            long currentBalance = account.balance();

            if (currentBalance < amount) {
                return false; // Insufficient funds — not a retry-able condition
            }

            long newBalance = currentBalance - amount;

            if (storage.updateBalance(playerId, newBalance, currentBalance)) {
                storage.logTransaction(EconomyTransaction.create(
                        playerId, -amount, newBalance, type, description));
                return true;
            }
            // Optimistic lock failed, retry (balance may have changed)
            logger.fine("Optimistic lock failed for withdrawal from " + playerId + ", attempt " + (attempt + 1));
        }

        logger.warning("Failed to withdraw from " + playerId + " after " + MAX_RETRIES + " retries");
        return false;
    }

    /**
     * Transfers coins between two players atomically (via optimistic retry).
     *
     * @param fromId the sender's UUID
     * @param toId   the recipient's UUID
     * @param amount the amount to transfer (must be positive)
     * @return true if the transfer succeeded
     * @throws IllegalArgumentException if amount is not positive or sender equals recipient
     */
    public boolean transfer(UUID fromId, UUID toId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive, got: " + amount);
        }
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("Cannot transfer to yourself");
        }

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            Optional<PlayerAccount> fromOpt = storage.getAccount(fromId);
            Optional<PlayerAccount> toOpt = storage.getAccount(toId);

            if (fromOpt.isEmpty() || toOpt.isEmpty()) {
                logger.warning("Transfer failed: one or both accounts don't exist");
                return false;
            }

            long fromBalance = fromOpt.get().balance();
            long toBalance = toOpt.get().balance();

            if (fromBalance < amount) {
                return false; // Insufficient funds
            }

            long newFromBalance = fromBalance - amount;
            long newToBalance = toBalance + amount;

            // Withdraw from sender first
            if (!storage.updateBalance(fromId, newFromBalance, fromBalance)) {
                logger.fine("Transfer optimistic lock failed on sender, attempt " + (attempt + 1));
                continue;
            }

            // Deposit to recipient
            if (!storage.updateBalance(toId, newToBalance, toBalance)) {
                // Rollback sender's balance
                logger.fine("Transfer optimistic lock failed on recipient, rolling back sender");
                if (!storage.updateBalance(fromId, fromBalance, newFromBalance)) {
                    logger.severe("CRITICAL: Failed to rollback sender balance for " + fromId +
                                  ". Expected: " + newFromBalance + ", tried to restore: " + fromBalance);
                }
                continue;
            }

            // Both updates succeeded — log transactions
            String senderName = fromOpt.get().playerName();
            String recipientName = toOpt.get().playerName();

            storage.logTransaction(EconomyTransaction.create(
                    fromId, -amount, newFromBalance, TransactionType.PLAYER_PAY,
                    "Paid " + amount + " to " + recipientName));
            storage.logTransaction(EconomyTransaction.create(
                    toId, amount, newToBalance, TransactionType.PLAYER_PAY,
                    "Received " + amount + " from " + senderName));
            return true;
        }

        logger.warning("Failed to transfer from " + fromId + " to " + toId + " after " + MAX_RETRIES + " retries");
        return false;
    }

    /**
     * Sets a player's balance directly (admin action).
     *
     * @param playerId the player's UUID
     * @param amount   the new balance (must be non-negative)
     * @return true if the balance was set successfully
     */
    public boolean setBalance(UUID playerId, long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Balance must be non-negative, got: " + amount);
        }

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            Optional<PlayerAccount> accountOpt = storage.getAccount(playerId);
            if (accountOpt.isEmpty()) {
                logger.warning("Cannot set balance for non-existent account: " + playerId);
                return false;
            }

            long currentBalance = accountOpt.get().balance();
            if (storage.updateBalance(playerId, amount, currentBalance)) {
                long diff = amount - currentBalance;
                storage.logTransaction(EconomyTransaction.create(
                        playerId, diff, amount, TransactionType.ADMIN_SET,
                        "Balance set to " + amount));
                return true;
            }
            logger.fine("Optimistic lock failed for setBalance on " + playerId + ", attempt " + (attempt + 1));
        }

        logger.warning("Failed to set balance for " + playerId + " after " + MAX_RETRIES + " retries");
        return false;
    }

    /**
     * Returns the top balances for the leaderboard.
     *
     * @param limit maximum number of results
     * @return list of accounts sorted by balance descending
     */
    public List<PlayerAccount> getTopBalances(int limit) {
        return storage.getTopBalances(limit);
    }

    /**
     * Updates the player's last daily reward claim.
     */
    public void updateLastDailyReward(UUID playerId) {
        storage.updateLastDailyReward(playerId);
    }

    /**
     * Formats a balance amount with the currency symbol.
     */
    public String formatBalance(long amount) {
        return configService.formatAmount(amount);
    }
}
