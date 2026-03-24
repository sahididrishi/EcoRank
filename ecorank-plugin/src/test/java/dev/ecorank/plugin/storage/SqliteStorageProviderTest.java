package dev.ecorank.plugin.storage;

import dev.ecorank.plugin.model.EconomyTransaction;
import dev.ecorank.plugin.model.PlayerAccount;
import dev.ecorank.plugin.model.TransactionType;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests SqliteStorageProvider using an in-memory SQLite database.
 */
class SqliteStorageProviderTest {

    private SqliteStorageProvider storage;
    private static final Logger LOGGER = Logger.getLogger(SqliteStorageProviderTest.class.getName());

    @BeforeEach
    void setUp() throws Exception {
        // Use a temp file for SQLite (in-memory via file path trick)
        File tempFile = File.createTempFile("ecorank-test-", ".db");
        tempFile.deleteOnExit();
        storage = new SqliteStorageProvider(LOGGER, tempFile);
        storage.initialize();
    }

    @AfterEach
    void tearDown() {
        if (storage != null) {
            storage.shutdown();
        }
    }

    @Test
    @DisplayName("getOrCreateAccount creates new account with starting balance")
    void testCreateAccount() {
        UUID playerId = UUID.randomUUID();
        PlayerAccount account = storage.getOrCreateAccount(playerId, "TestPlayer", 100);

        assertNotNull(account);
        assertEquals(playerId, account.playerId());
        assertEquals("TestPlayer", account.playerName());
        assertEquals(100, account.balance());
        assertNotNull(account.lastLogin());
        assertNull(account.lastDailyReward());
    }

    @Test
    @DisplayName("getOrCreateAccount returns existing account without modifying balance")
    void testGetExistingAccount() {
        UUID playerId = UUID.randomUUID();
        storage.getOrCreateAccount(playerId, "TestPlayer", 100);

        // Call again with different starting balance — should return existing
        PlayerAccount existing = storage.getOrCreateAccount(playerId, "TestPlayer", 500);
        assertEquals(100, existing.balance()); // Original balance preserved
    }

    @Test
    @DisplayName("getAccount returns empty for non-existent player")
    void testGetNonExistentAccount() {
        Optional<PlayerAccount> account = storage.getAccount(UUID.randomUUID());
        assertTrue(account.isEmpty());
    }

    @Test
    @DisplayName("getAccount returns existing account")
    void testGetAccount() {
        UUID playerId = UUID.randomUUID();
        storage.getOrCreateAccount(playerId, "TestPlayer", 200);

        Optional<PlayerAccount> account = storage.getAccount(playerId);
        assertTrue(account.isPresent());
        assertEquals(200, account.get().balance());
        assertEquals("TestPlayer", account.get().playerName());
    }

    @Test
    @DisplayName("updateBalance succeeds with correct expected balance")
    void testUpdateBalanceSuccess() {
        UUID playerId = UUID.randomUUID();
        storage.getOrCreateAccount(playerId, "TestPlayer", 100);

        boolean updated = storage.updateBalance(playerId, 250, 100);
        assertTrue(updated);

        Optional<PlayerAccount> account = storage.getAccount(playerId);
        assertTrue(account.isPresent());
        assertEquals(250, account.get().balance());
    }

    @Test
    @DisplayName("updateBalance fails with wrong expected balance (optimistic concurrency)")
    void testUpdateBalanceOptimisticLockFailure() {
        UUID playerId = UUID.randomUUID();
        storage.getOrCreateAccount(playerId, "TestPlayer", 100);

        // Wrong expected balance — should fail
        boolean updated = storage.updateBalance(playerId, 250, 50);
        assertFalse(updated);

        // Balance should be unchanged
        Optional<PlayerAccount> account = storage.getAccount(playerId);
        assertTrue(account.isPresent());
        assertEquals(100, account.get().balance());
    }

    @Test
    @DisplayName("updateBalance handles concurrent modifications correctly")
    void testOptimisticConcurrency() {
        UUID playerId = UUID.randomUUID();
        storage.getOrCreateAccount(playerId, "TestPlayer", 100);

        // Simulate two concurrent reads
        long readBalance1 = storage.getAccount(playerId).get().balance(); // 100
        long readBalance2 = storage.getAccount(playerId).get().balance(); // 100

        // First update succeeds
        boolean first = storage.updateBalance(playerId, readBalance1 + 50, readBalance1);
        assertTrue(first);

        // Second update fails because balance is now 150, not 100
        boolean second = storage.updateBalance(playerId, readBalance2 + 30, readBalance2);
        assertFalse(second);

        // Final balance should be 150 (only first update applied)
        assertEquals(150, storage.getAccount(playerId).get().balance());
    }

    @Test
    @DisplayName("logTransaction records transaction in database")
    void testLogTransaction() {
        UUID playerId = UUID.randomUUID();
        storage.getOrCreateAccount(playerId, "TestPlayer", 100);

        EconomyTransaction tx = EconomyTransaction.create(
                playerId, 50, 150, TransactionType.MOB_KILL, "Killed Zombie");

        // Should not throw
        assertDoesNotThrow(() -> storage.logTransaction(tx));
    }

    @Test
    @DisplayName("getTopBalances returns accounts sorted by balance descending")
    void testGetTopBalances() {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        UUID player3 = UUID.randomUUID();

        storage.getOrCreateAccount(player1, "Rich", 1000);
        storage.getOrCreateAccount(player2, "Middle", 500);
        storage.getOrCreateAccount(player3, "Poor", 100);

        List<PlayerAccount> top = storage.getTopBalances(10);
        assertEquals(3, top.size());
        assertEquals("Rich", top.get(0).playerName());
        assertEquals(1000, top.get(0).balance());
        assertEquals("Middle", top.get(1).playerName());
        assertEquals("Poor", top.get(2).playerName());
    }

    @Test
    @DisplayName("getTopBalances respects limit parameter")
    void testGetTopBalancesLimit() {
        for (int i = 0; i < 5; i++) {
            storage.getOrCreateAccount(UUID.randomUUID(), "Player" + i, i * 100);
        }

        List<PlayerAccount> top = storage.getTopBalances(3);
        assertEquals(3, top.size());
    }

    @Test
    @DisplayName("updateLastDailyReward sets the timestamp")
    void testUpdateLastDailyReward() {
        UUID playerId = UUID.randomUUID();
        storage.getOrCreateAccount(playerId, "TestPlayer", 100);

        // Initially null
        assertNull(storage.getAccount(playerId).get().lastDailyReward());

        storage.updateLastDailyReward(playerId);

        assertNotNull(storage.getAccount(playerId).get().lastDailyReward());
    }

    @Test
    @DisplayName("updateLastLogin updates login timestamp and player name")
    void testUpdateLastLogin() {
        UUID playerId = UUID.randomUUID();
        storage.getOrCreateAccount(playerId, "OldName", 100);

        storage.updateLastLogin(playerId, "NewName");

        Optional<PlayerAccount> account = storage.getAccount(playerId);
        assertTrue(account.isPresent());
        assertEquals("NewName", account.get().playerName());
    }

    @Test
    @DisplayName("Multiple accounts are independent")
    void testMultipleAccounts() {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        storage.getOrCreateAccount(player1, "Alice", 100);
        storage.getOrCreateAccount(player2, "Bob", 200);

        // Modify one
        storage.updateBalance(player1, 150, 100);

        // Other is unchanged
        assertEquals(150, storage.getAccount(player1).get().balance());
        assertEquals(200, storage.getAccount(player2).get().balance());
    }
}
