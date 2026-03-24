package dev.ecorank.plugin.service;

import dev.ecorank.plugin.config.ConfigService;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests ConfigService fail-fast behavior and typed getters.
 * Uses YamlConfiguration (in-memory, no file I/O) as it's a concrete FileConfiguration.
 */
class ConfigServiceTest {

    private YamlConfiguration config;

    @BeforeEach
    void setUp() {
        config = new YamlConfiguration();
        // Set minimum required values
        config.set("backend.url", "http://localhost:8080");
        config.set("backend.api-key", "test-api-key-123");
    }

    @Test
    @DisplayName("ConfigService initializes with valid required values")
    void testValidConfig() {
        assertDoesNotThrow(() -> new ConfigService(config));
    }

    @Test
    @DisplayName("Missing backend.url throws IllegalStateException")
    void testMissingBackendUrl() {
        config.set("backend.url", null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new ConfigService(config));
        assertTrue(ex.getMessage().contains("backend.url"));
    }

    @Test
    @DisplayName("Missing backend.api-key throws IllegalStateException")
    void testMissingBackendApiKey() {
        config.set("backend.api-key", null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new ConfigService(config));
        assertTrue(ex.getMessage().contains("backend.api-key"));
    }

    @Test
    @DisplayName("CHANGE_ME api-key throws IllegalStateException")
    void testChangeMeApiKey() {
        config.set("backend.api-key", "CHANGE_ME");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new ConfigService(config));
        assertTrue(ex.getMessage().contains("backend.api-key"));
    }

    @Test
    @DisplayName("Blank backend.url throws IllegalStateException")
    void testBlankBackendUrl() {
        config.set("backend.url", "   ");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new ConfigService(config));
        assertTrue(ex.getMessage().contains("backend.url"));
    }

    @Test
    @DisplayName("Default values are returned for optional config")
    void testDefaultValues() {
        ConfigService svc = new ConfigService(config);

        assertEquals("sqlite", svc.getStorageType());
        assertEquals("ecorank.db", svc.getSqliteFile());
        assertEquals(0, svc.getStartingBalance());
        assertEquals("Coin", svc.getCurrencyName());
        assertEquals("Coins", svc.getCurrencyNamePlural());
        assertEquals("$", svc.getCurrencySymbol());
        assertEquals(5, svc.getPollIntervalSeconds());
        assertEquals(30, svc.getMaxBackoffSeconds());
        assertEquals("main", svc.getServerId());
        assertTrue(svc.isMobKillEnabled());
        assertTrue(svc.isDailyLoginEnabled());
        assertTrue(svc.isChestQuestEnabled());
        assertEquals(100, svc.getDailyLoginAmount());
        assertEquals(24, svc.getDailyLoginCooldownHours());
        assertEquals(25, svc.getChestQuestAmount());
        assertEquals(300, svc.getChestQuestCooldownSeconds());
        assertEquals("default", svc.getLuckpermsTrack());
    }

    @Test
    @DisplayName("Custom values override defaults")
    void testCustomValues() {
        config.set("storage.type", "mysql");
        config.set("economy.starting-balance", 500);
        config.set("economy.currency-name", "Gold");
        config.set("economy.currency-name-plural", "Gold");
        config.set("economy.currency-symbol", "G");
        config.set("backend.poll-interval-seconds", 10);
        config.set("backend.max-backoff-seconds", 60);

        ConfigService svc = new ConfigService(config);

        assertEquals("mysql", svc.getStorageType());
        assertEquals(500, svc.getStartingBalance());
        assertEquals("Gold", svc.getCurrencyName());
        assertEquals("G", svc.getCurrencySymbol());
        assertEquals(10, svc.getPollIntervalSeconds());
        assertEquals(60, svc.getMaxBackoffSeconds());
    }

    @Test
    @DisplayName("formatAmount prepends currency symbol")
    void testFormatAmount() {
        ConfigService svc = new ConfigService(config);
        assertEquals("$150", svc.formatAmount(150));
        assertEquals("$0", svc.formatAmount(0));
    }

    @Test
    @DisplayName("formatAmountWithName uses singular/plural correctly")
    void testFormatAmountWithName() {
        ConfigService svc = new ConfigService(config);
        assertEquals("1 Coin", svc.formatAmountWithName(1));
        assertEquals("5 Coins", svc.formatAmountWithName(5));
        assertEquals("0 Coins", svc.formatAmountWithName(0));
    }

    @Test
    @DisplayName("Mob kill per-type rewards are loaded from config")
    void testMobKillPerType() {
        config.set("rewards.mob-kill.default", 10);
        config.set("rewards.mob-kill.per-type.ZOMBIE", 5);
        config.set("rewards.mob-kill.per-type.CREEPER", 15);

        ConfigService svc = new ConfigService(config);

        assertEquals(10, svc.getMobKillDefault());
        assertEquals(2, svc.getMobKillPerType().size());
    }

    @Test
    @DisplayName("getMobKillReward falls back to default for unconfigured types")
    void testMobKillRewardFallback() {
        config.set("rewards.mob-kill.default", 10);
        config.set("rewards.mob-kill.per-type.ZOMBIE", 5);

        ConfigService svc = new ConfigService(config);

        assertEquals(5, svc.getMobKillReward(org.bukkit.entity.EntityType.ZOMBIE));
        assertEquals(10, svc.getMobKillReward(org.bukkit.entity.EntityType.SKELETON));
    }

    @Test
    @DisplayName("MySQL config values are accessible")
    void testMysqlConfig() {
        config.set("storage.mysql.host", "db.example.com");
        config.set("storage.mysql.port", 3307);
        config.set("storage.mysql.database", "mydb");
        config.set("storage.mysql.username", "admin");
        config.set("storage.mysql.password", "secret");
        config.set("storage.mysql.pool-size", 20);

        ConfigService svc = new ConfigService(config);

        assertEquals("db.example.com", svc.getMysqlHost());
        assertEquals(3307, svc.getMysqlPort());
        assertEquals("mydb", svc.getMysqlDatabase());
        assertEquals("admin", svc.getMysqlUsername());
        assertEquals("secret", svc.getMysqlPassword());
        assertEquals(20, svc.getMysqlPoolSize());
    }
}
