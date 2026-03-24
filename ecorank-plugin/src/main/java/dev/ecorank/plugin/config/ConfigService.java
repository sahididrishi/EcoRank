package dev.ecorank.plugin.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

/**
 * Typed wrapper around Paper's FileConfiguration.
 * Fails fast on missing required configuration values at startup.
 */
public class ConfigService {

    private final FileConfiguration config;

    // Storage
    private final String storageType;
    private final String sqliteFile;
    private final String mysqlHost;
    private final int mysqlPort;
    private final String mysqlDatabase;
    private final String mysqlUsername;
    private final String mysqlPassword;
    private final int mysqlPoolSize;

    // Backend
    private final String backendUrl;
    private final String backendApiKey;
    private final String serverId;
    private final int pollIntervalSeconds;
    private final int maxBackoffSeconds;

    // Economy
    private final long startingBalance;
    private final String currencyName;
    private final String currencyNamePlural;
    private final String currencySymbol;

    // Rewards - Mob Kill
    private final boolean mobKillEnabled;
    private final long mobKillDefault;
    private final Map<EntityType, Long> mobKillPerType;

    // Rewards - Daily Login
    private final boolean dailyLoginEnabled;
    private final long dailyLoginAmount;
    private final int dailyLoginCooldownHours;

    // Rewards - Chest Quest
    private final boolean chestQuestEnabled;
    private final long chestQuestAmount;
    private final int chestQuestCooldownSeconds;

    // Ranks
    private final String luckpermsTrack;

    public ConfigService(FileConfiguration config) {
        this.config = config;

        // Storage
        this.storageType = config.getString("storage.type", "sqlite");
        this.sqliteFile = config.getString("storage.sqlite.file", "ecorank.db");
        this.mysqlHost = config.getString("storage.mysql.host", "localhost");
        this.mysqlPort = config.getInt("storage.mysql.port", 3306);
        this.mysqlDatabase = config.getString("storage.mysql.database", "ecorank");
        this.mysqlUsername = config.getString("storage.mysql.username", "root");
        this.mysqlPassword = config.getString("storage.mysql.password", "");
        this.mysqlPoolSize = config.getInt("storage.mysql.pool-size", 10);

        // Backend (required values)
        this.backendUrl = requireString("backend.url");
        this.backendApiKey = requireString("backend.api-key");
        this.serverId = config.getString("backend.server-id", "main");
        this.pollIntervalSeconds = config.getInt("backend.poll-interval-seconds", 5);
        this.maxBackoffSeconds = config.getInt("backend.max-backoff-seconds", 30);

        // Economy
        this.startingBalance = config.getLong("economy.starting-balance", 0);
        this.currencyName = config.getString("economy.currency-name", "Coin");
        this.currencyNamePlural = config.getString("economy.currency-name-plural", "Coins");
        this.currencySymbol = config.getString("economy.currency-symbol", "$");

        // Rewards - Mob Kill
        this.mobKillEnabled = config.getBoolean("rewards.mob-kill.enabled", true);
        this.mobKillDefault = config.getLong("rewards.mob-kill.default", 5);
        this.mobKillPerType = loadMobKillRewards();

        // Rewards - Daily Login
        this.dailyLoginEnabled = config.getBoolean("rewards.daily-login.enabled", true);
        this.dailyLoginAmount = config.getLong("rewards.daily-login.amount", 100);
        this.dailyLoginCooldownHours = config.getInt("rewards.daily-login.cooldown-hours", 24);

        // Rewards - Chest Quest
        this.chestQuestEnabled = config.getBoolean("rewards.chest-quest.enabled", true);
        this.chestQuestAmount = config.getLong("rewards.chest-quest.amount", 25);
        this.chestQuestCooldownSeconds = config.getInt("rewards.chest-quest.cooldown-seconds", 300);

        // Ranks
        this.luckpermsTrack = config.getString("ranks.luckperms-track", "default");
    }

    /**
     * Requires a non-null, non-blank string from config. Fails fast if missing.
     */
    private String requireString(String path) {
        String value = config.getString(path);
        if (value == null || value.isBlank() || "CHANGE_ME".equals(value)) {
            throw new IllegalStateException(
                    "Missing required configuration value: " + path +
                    ". Please set this in config.yml before starting EcoRank.");
        }
        return value;
    }

    private Map<EntityType, Long> loadMobKillRewards() {
        Map<EntityType, Long> rewards = new HashMap<>();
        var section = config.getConfigurationSection("rewards.mob-kill.per-type");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    EntityType type = EntityType.valueOf(key.toUpperCase());
                    rewards.put(type, section.getLong(key));
                } catch (IllegalArgumentException e) {
                    // Skip invalid entity types silently — server admin may have typos
                }
            }
        }
        return Map.copyOf(rewards);
    }

    // --- Storage getters ---

    public String getStorageType() { return storageType; }
    public String getSqliteFile() { return sqliteFile; }
    public String getMysqlHost() { return mysqlHost; }
    public int getMysqlPort() { return mysqlPort; }
    public String getMysqlDatabase() { return mysqlDatabase; }
    public String getMysqlUsername() { return mysqlUsername; }
    public String getMysqlPassword() { return mysqlPassword; }
    public int getMysqlPoolSize() { return mysqlPoolSize; }

    // --- Backend getters ---

    public String getBackendUrl() { return backendUrl; }
    public String getBackendApiKey() { return backendApiKey; }
    public String getServerId() { return serverId; }
    public int getPollIntervalSeconds() { return pollIntervalSeconds; }
    public int getMaxBackoffSeconds() { return maxBackoffSeconds; }

    // --- Economy getters ---

    public long getStartingBalance() { return startingBalance; }
    public String getCurrencyName() { return currencyName; }
    public String getCurrencyNamePlural() { return currencyNamePlural; }
    public String getCurrencySymbol() { return currencySymbol; }

    /**
     * Formats an amount with the currency symbol (e.g., "$150").
     */
    public String formatAmount(long amount) {
        return currencySymbol + amount;
    }

    /**
     * Formats an amount with the full currency name (e.g., "150 Coins").
     */
    public String formatAmountWithName(long amount) {
        String name = (amount == 1) ? currencyName : currencyNamePlural;
        return amount + " " + name;
    }

    // --- Reward getters ---

    public boolean isMobKillEnabled() { return mobKillEnabled; }
    public long getMobKillDefault() { return mobKillDefault; }
    public Map<EntityType, Long> getMobKillPerType() { return mobKillPerType; }

    /**
     * Returns the reward amount for a specific mob type, falling back to the default.
     */
    public long getMobKillReward(EntityType type) {
        return mobKillPerType.getOrDefault(type, mobKillDefault);
    }

    public boolean isDailyLoginEnabled() { return dailyLoginEnabled; }
    public long getDailyLoginAmount() { return dailyLoginAmount; }
    public int getDailyLoginCooldownHours() { return dailyLoginCooldownHours; }

    public boolean isChestQuestEnabled() { return chestQuestEnabled; }
    public long getChestQuestAmount() { return chestQuestAmount; }
    public int getChestQuestCooldownSeconds() { return chestQuestCooldownSeconds; }

    // --- Rank getters ---

    public String getLuckpermsTrack() { return luckpermsTrack; }
}
