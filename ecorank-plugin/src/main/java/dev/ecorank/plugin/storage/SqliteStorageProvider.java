package dev.ecorank.plugin.storage;

import com.zaxxer.hikari.HikariConfig;

import java.io.File;
import java.util.logging.Logger;

/**
 * SQLite storage provider. Uses WAL mode for better concurrency.
 * Connection pool size is fixed at 1 since SQLite only supports single-writer.
 */
public class SqliteStorageProvider extends SqlStorageProvider {

    private final File databaseFile;

    public SqliteStorageProvider(Logger logger, File databaseFile) {
        super(logger);
        this.databaseFile = databaseFile;
    }

    @Override
    protected void configureHikari(HikariConfig config) {
        config.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        // SQLite only supports single-writer concurrency
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        config.setPoolName("EcoRank-SQLite");
        // Enable WAL mode and foreign keys via connection init
        config.addDataSourceProperty("journal_mode", "WAL");
        config.setConnectionInitSql("PRAGMA journal_mode=WAL; PRAGMA foreign_keys=ON;");
    }

    @Override
    protected String[] getCreateTablesSql() {
        return new String[]{
            """
            CREATE TABLE IF NOT EXISTS player_accounts (
                player_id   TEXT PRIMARY KEY,
                player_name TEXT NOT NULL,
                balance     INTEGER NOT NULL DEFAULT 0,
                last_login  TIMESTAMP,
                last_daily_reward TIMESTAMP
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS economy_transactions (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                player_id    TEXT NOT NULL,
                amount       INTEGER NOT NULL,
                balance_after INTEGER NOT NULL,
                type         TEXT NOT NULL,
                description  TEXT DEFAULT '',
                timestamp    TIMESTAMP NOT NULL,
                FOREIGN KEY (player_id) REFERENCES player_accounts(player_id)
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_transactions_player ON economy_transactions(player_id)",
            "CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON economy_transactions(timestamp)"
        };
    }
}
