package dev.ecorank.plugin.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.ecorank.plugin.model.EconomyTransaction;
import dev.ecorank.plugin.model.PlayerAccount;
import dev.ecorank.plugin.model.TransactionType;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract SQL-based storage provider using HikariCP connection pooling.
 * Subclasses provide database-specific configuration and DDL.
 */
public abstract class SqlStorageProvider implements StorageProvider {

    protected final Logger logger;
    protected HikariDataSource dataSource;

    protected SqlStorageProvider(Logger logger) {
        this.logger = logger;
    }

    /**
     * Returns the SQL statements to create required tables.
     * Called once during initialization.
     */
    protected abstract String[] getCreateTablesSql();

    /**
     * Configures the HikariCP pool settings specific to the database engine.
     */
    protected abstract void configureHikari(HikariConfig hikariConfig);

    @Override
    public void initialize() throws Exception {
        HikariConfig hikariConfig = new HikariConfig();
        configureHikari(hikariConfig);
        dataSource = new HikariDataSource(hikariConfig);

        // Create tables
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : getCreateTablesSql()) {
                stmt.execute(sql);
            }
        }
        logger.info("Storage initialized successfully.");
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Storage connection pool closed.");
        }
    }

    @Override
    public PlayerAccount getOrCreateAccount(UUID playerId, String playerName, long startingBalance) {
        Optional<PlayerAccount> existing = getAccount(playerId);
        if (existing.isPresent()) {
            return existing.get();
        }

        String sql = "INSERT INTO player_accounts (player_id, player_name, balance, last_login, last_daily_reward) " +
                     "VALUES (?, ?, ?, ?, NULL)";
        Instant now = Instant.now();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, playerName);
            ps.setLong(3, startingBalance);
            ps.setTimestamp(4, Timestamp.from(now));
            ps.executeUpdate();
        } catch (SQLException e) {
            // Race condition: another thread may have inserted between our SELECT and INSERT
            logger.log(Level.FINE, "Insert race condition (expected if concurrent), retrying read", e);
            Optional<PlayerAccount> retried = getAccount(playerId);
            if (retried.isPresent()) {
                return retried.get();
            }
            throw new RuntimeException("Failed to create account for " + playerId, e);
        }

        return new PlayerAccount(playerId, playerName, startingBalance, now, null);
    }

    @Override
    public Optional<PlayerAccount> getAccount(UUID playerId) {
        String sql = "SELECT player_id, player_name, balance, last_login, last_daily_reward " +
                     "FROM player_accounts WHERE player_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapAccount(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get account for " + playerId, e);
        }
        return Optional.empty();
    }

    @Override
    public boolean updateBalance(UUID playerId, long newBalance, long expectedBalance) {
        String sql = "UPDATE player_accounts SET balance = ? WHERE player_id = ? AND balance = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, newBalance);
            ps.setString(2, playerId.toString());
            ps.setLong(3, expectedBalance);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to update balance for " + playerId, e);
            return false;
        }
    }

    @Override
    public void logTransaction(EconomyTransaction transaction) {
        String sql = "INSERT INTO economy_transactions (player_id, amount, balance_after, type, description, timestamp) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, transaction.playerId().toString());
            ps.setLong(2, transaction.amount());
            ps.setLong(3, transaction.balanceAfter());
            ps.setString(4, transaction.type().name());
            ps.setString(5, transaction.description());
            ps.setTimestamp(6, Timestamp.from(transaction.timestamp()));
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to log transaction for " + transaction.playerId(), e);
        }
    }

    @Override
    public List<PlayerAccount> getTopBalances(int limit) {
        String sql = "SELECT player_id, player_name, balance, last_login, last_daily_reward " +
                     "FROM player_accounts ORDER BY balance DESC LIMIT ?";
        List<PlayerAccount> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapAccount(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get top balances", e);
        }
        return results;
    }

    @Override
    public void updateLastDailyReward(UUID playerId) {
        String sql = "UPDATE player_accounts SET last_daily_reward = ? WHERE player_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(Instant.now()));
            ps.setString(2, playerId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to update last daily reward for " + playerId, e);
        }
    }

    @Override
    public void updateLastLogin(UUID playerId, String playerName) {
        String sql = "UPDATE player_accounts SET last_login = ?, player_name = ? WHERE player_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(Instant.now()));
            ps.setString(2, playerName);
            ps.setString(3, playerId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to update last login for " + playerId, e);
        }
    }

    /**
     * Maps a ResultSet row to a PlayerAccount record.
     */
    private PlayerAccount mapAccount(ResultSet rs) throws SQLException {
        UUID playerId = UUID.fromString(rs.getString("player_id"));
        String playerName = rs.getString("player_name");
        long balance = rs.getLong("balance");

        Timestamp lastLoginTs = rs.getTimestamp("last_login");
        Instant lastLogin = lastLoginTs != null ? lastLoginTs.toInstant() : Instant.now();

        Timestamp lastDailyTs = rs.getTimestamp("last_daily_reward");
        Instant lastDailyReward = lastDailyTs != null ? lastDailyTs.toInstant() : null;

        return new PlayerAccount(playerId, playerName, balance, lastLogin, lastDailyReward);
    }
}
