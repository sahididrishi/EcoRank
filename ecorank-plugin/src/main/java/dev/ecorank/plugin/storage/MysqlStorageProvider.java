package dev.ecorank.plugin.storage;

import com.zaxxer.hikari.HikariConfig;
import dev.ecorank.plugin.config.ConfigService;

import java.util.logging.Logger;

/**
 * MySQL storage provider with configurable connection pool.
 * Uses utf8mb4 character set for full Unicode support.
 */
public class MysqlStorageProvider extends SqlStorageProvider {

    private final ConfigService configService;

    public MysqlStorageProvider(Logger logger, ConfigService configService) {
        super(logger);
        this.configService = configService;
    }

    @Override
    protected void configureHikari(HikariConfig config) {
        String jdbcUrl = String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4&useUnicode=true",
                configService.getMysqlHost(),
                configService.getMysqlPort(),
                configService.getMysqlDatabase()
        );
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(configService.getMysqlUsername());
        config.setPassword(configService.getMysqlPassword());
        config.setMaximumPoolSize(configService.getMysqlPoolSize());
        config.setMinimumIdle(2);
        config.setPoolName("EcoRank-MySQL");
        config.setConnectionTimeout(5000);
        config.setMaxLifetime(1800000); // 30 minutes
        config.setIdleTimeout(600000);  // 10 minutes
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
    }

    @Override
    protected String[] getCreateTablesSql() {
        return new String[]{
            """
            CREATE TABLE IF NOT EXISTS player_accounts (
                player_id   VARCHAR(36) PRIMARY KEY,
                player_name VARCHAR(16) NOT NULL,
                balance     BIGINT NOT NULL DEFAULT 0,
                last_login  TIMESTAMP NULL,
                last_daily_reward TIMESTAMP NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS economy_transactions (
                id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                player_id     VARCHAR(36) NOT NULL,
                amount        BIGINT NOT NULL,
                balance_after BIGINT NOT NULL,
                type          VARCHAR(32) NOT NULL,
                description   VARCHAR(255) DEFAULT '',
                timestamp     TIMESTAMP NOT NULL,
                FOREIGN KEY (player_id) REFERENCES player_accounts(player_id),
                INDEX idx_transactions_player (player_id),
                INDEX idx_transactions_timestamp (timestamp)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """
        };
    }
}
