package dev.ecorank.plugin.service;

import dev.ecorank.plugin.config.ConfigService;
import dev.ecorank.plugin.model.EconomyTransaction;
import dev.ecorank.plugin.model.PlayerAccount;
import dev.ecorank.plugin.model.TransactionType;
import dev.ecorank.plugin.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EconomyServiceTest {

    @Mock private StorageProvider storage;
    @Mock private ConfigService configService;

    private EconomyService economyService;
    private static final Logger LOGGER = Logger.getLogger(EconomyServiceTest.class.getName());

    private UUID playerId;
    private PlayerAccount testAccount;

    @BeforeEach
    void setUp() {
        economyService = new EconomyService(storage, configService, LOGGER);
        playerId = UUID.randomUUID();
        testAccount = new PlayerAccount(playerId, "TestPlayer", 500, Instant.now(), null);
    }

    // --- ensureAccount ---

    @Test
    @DisplayName("ensureAccount delegates to storage with starting balance")
    void testEnsureAccount() {
        when(configService.getStartingBalance()).thenReturn(100L);
        when(storage.getOrCreateAccount(playerId, "TestPlayer", 100)).thenReturn(testAccount);

        PlayerAccount result = economyService.ensureAccount(playerId, "TestPlayer");

        assertEquals(testAccount, result);
        verify(storage).getOrCreateAccount(playerId, "TestPlayer", 100);
    }

    // --- getBalance ---

    @Test
    @DisplayName("getBalance returns balance from existing account")
    void testGetBalance() {
        when(storage.getAccount(playerId)).thenReturn(Optional.of(testAccount));

        long balance = economyService.getBalance(playerId);
        assertEquals(500, balance);
    }

    @Test
    @DisplayName("getBalance returns 0 for non-existent account")
    void testGetBalanceNonExistent() {
        when(storage.getAccount(playerId)).thenReturn(Optional.empty());

        long balance = economyService.getBalance(playerId);
        assertEquals(0, balance);
    }

    // --- deposit ---

    @Test
    @DisplayName("deposit adds amount to balance and logs transaction")
    void testDeposit() {
        when(storage.getAccount(playerId)).thenReturn(Optional.of(testAccount));
        when(storage.updateBalance(playerId, 600, 500)).thenReturn(true);

        boolean result = economyService.deposit(playerId, 100, TransactionType.MOB_KILL, "Killed Zombie");

        assertTrue(result);
        verify(storage).updateBalance(playerId, 600, 500);
        verify(storage).logTransaction(any(EconomyTransaction.class));
    }

    @Test
    @DisplayName("deposit fails for non-existent account")
    void testDepositNonExistent() {
        when(storage.getAccount(playerId)).thenReturn(Optional.empty());

        boolean result = economyService.deposit(playerId, 100, TransactionType.ADMIN_GIVE, "test");
        assertFalse(result);
    }

    @Test
    @DisplayName("deposit throws on non-positive amount")
    void testDepositNonPositive() {
        assertThrows(IllegalArgumentException.class, () ->
                economyService.deposit(playerId, 0, TransactionType.ADMIN_GIVE, "test"));
        assertThrows(IllegalArgumentException.class, () ->
                economyService.deposit(playerId, -10, TransactionType.ADMIN_GIVE, "test"));
    }

    @Test
    @DisplayName("deposit retries on optimistic lock failure")
    void testDepositRetry() {
        when(storage.getAccount(playerId)).thenReturn(Optional.of(testAccount));
        // First attempt fails, second succeeds
        when(storage.updateBalance(playerId, 600, 500))
                .thenReturn(false)
                .thenReturn(true);

        boolean result = economyService.deposit(playerId, 100, TransactionType.MOB_KILL, "test");
        assertTrue(result);
        verify(storage, times(2)).updateBalance(playerId, 600, 500);
    }

    @Test
    @DisplayName("deposit fails after max retries exhausted")
    void testDepositMaxRetries() {
        when(storage.getAccount(playerId)).thenReturn(Optional.of(testAccount));
        when(storage.updateBalance(playerId, 600, 500)).thenReturn(false);

        boolean result = economyService.deposit(playerId, 100, TransactionType.ADMIN_GIVE, "test");
        assertFalse(result);
        verify(storage, times(3)).updateBalance(playerId, 600, 500);
    }

    // --- withdraw ---

    @Test
    @DisplayName("withdraw removes amount from balance and logs transaction")
    void testWithdraw() {
        when(storage.getAccount(playerId)).thenReturn(Optional.of(testAccount));
        when(storage.updateBalance(playerId, 400, 500)).thenReturn(true);

        boolean result = economyService.withdraw(playerId, 100, TransactionType.ADMIN_TAKE, "Admin take");

        assertTrue(result);
        verify(storage).updateBalance(playerId, 400, 500);
        verify(storage).logTransaction(any(EconomyTransaction.class));
    }

    @Test
    @DisplayName("withdraw fails with insufficient funds")
    void testWithdrawInsufficientFunds() {
        when(storage.getAccount(playerId)).thenReturn(Optional.of(testAccount));

        boolean result = economyService.withdraw(playerId, 600, TransactionType.ADMIN_TAKE, "test");
        assertFalse(result);
        verify(storage, never()).updateBalance(any(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("withdraw throws on non-positive amount")
    void testWithdrawNonPositive() {
        assertThrows(IllegalArgumentException.class, () ->
                economyService.withdraw(playerId, 0, TransactionType.ADMIN_TAKE, "test"));
    }

    @Test
    @DisplayName("withdraw fails for non-existent account")
    void testWithdrawNonExistent() {
        when(storage.getAccount(playerId)).thenReturn(Optional.empty());

        boolean result = economyService.withdraw(playerId, 100, TransactionType.ADMIN_TAKE, "test");
        assertFalse(result);
    }

    // --- transfer ---

    @Test
    @DisplayName("transfer moves coins between two players")
    void testTransfer() {
        UUID recipientId = UUID.randomUUID();
        PlayerAccount recipientAccount = new PlayerAccount(recipientId, "Recipient", 200, Instant.now(), null);

        when(storage.getAccount(playerId)).thenReturn(Optional.of(testAccount));
        when(storage.getAccount(recipientId)).thenReturn(Optional.of(recipientAccount));
        when(storage.updateBalance(playerId, 400, 500)).thenReturn(true);
        when(storage.updateBalance(recipientId, 300, 200)).thenReturn(true);

        boolean result = economyService.transfer(playerId, recipientId, 100);

        assertTrue(result);
        verify(storage).updateBalance(playerId, 400, 500);
        verify(storage).updateBalance(recipientId, 300, 200);
        verify(storage, times(2)).logTransaction(any(EconomyTransaction.class));
    }

    @Test
    @DisplayName("transfer fails with insufficient funds")
    void testTransferInsufficientFunds() {
        UUID recipientId = UUID.randomUUID();
        PlayerAccount recipientAccount = new PlayerAccount(recipientId, "Recipient", 200, Instant.now(), null);

        when(storage.getAccount(playerId)).thenReturn(Optional.of(testAccount));
        when(storage.getAccount(recipientId)).thenReturn(Optional.of(recipientAccount));

        boolean result = economyService.transfer(playerId, recipientId, 600);
        assertFalse(result);
    }

    @Test
    @DisplayName("transfer throws when sending to self")
    void testTransferToSelf() {
        assertThrows(IllegalArgumentException.class, () ->
                economyService.transfer(playerId, playerId, 100));
    }

    @Test
    @DisplayName("transfer throws on non-positive amount")
    void testTransferNonPositive() {
        assertThrows(IllegalArgumentException.class, () ->
                economyService.transfer(playerId, UUID.randomUUID(), 0));
    }

    @Test
    @DisplayName("transfer rolls back sender on recipient update failure")
    void testTransferRollback() {
        UUID recipientId = UUID.randomUUID();
        PlayerAccount recipientAccount = new PlayerAccount(recipientId, "Recipient", 200, Instant.now(), null);

        when(storage.getAccount(playerId)).thenReturn(Optional.of(testAccount));
        when(storage.getAccount(recipientId)).thenReturn(Optional.of(recipientAccount));
        // Sender debit succeeds
        when(storage.updateBalance(playerId, 400, 500)).thenReturn(true);
        // Recipient credit fails
        when(storage.updateBalance(recipientId, 300, 200)).thenReturn(false);
        // Rollback succeeds
        when(storage.updateBalance(playerId, 500, 400)).thenReturn(true);

        // All 3 retries will follow same pattern since mocks return same values
        boolean result = economyService.transfer(playerId, recipientId, 100);
        assertFalse(result);

        // Verify rollback was attempted
        verify(storage, atLeastOnce()).updateBalance(playerId, 500, 400);
    }

    // --- setBalance ---

    @Test
    @DisplayName("setBalance sets exact amount and logs transaction")
    void testSetBalance() {
        when(storage.getAccount(playerId)).thenReturn(Optional.of(testAccount));
        when(storage.updateBalance(playerId, 1000, 500)).thenReturn(true);

        boolean result = economyService.setBalance(playerId, 1000);

        assertTrue(result);
        verify(storage).updateBalance(playerId, 1000, 500);
        verify(storage).logTransaction(any(EconomyTransaction.class));
    }

    @Test
    @DisplayName("setBalance throws on negative amount")
    void testSetBalanceNegative() {
        assertThrows(IllegalArgumentException.class, () ->
                economyService.setBalance(playerId, -1));
    }

    @Test
    @DisplayName("setBalance allows zero")
    void testSetBalanceZero() {
        when(storage.getAccount(playerId)).thenReturn(Optional.of(testAccount));
        when(storage.updateBalance(playerId, 0, 500)).thenReturn(true);

        boolean result = economyService.setBalance(playerId, 0);
        assertTrue(result);
    }

    // --- getTopBalances ---

    @Test
    @DisplayName("getTopBalances delegates to storage")
    void testGetTopBalances() {
        List<PlayerAccount> expected = List.of(testAccount);
        when(storage.getTopBalances(10)).thenReturn(expected);

        List<PlayerAccount> result = economyService.getTopBalances(10);
        assertEquals(expected, result);
    }

    // --- formatBalance ---

    @Test
    @DisplayName("formatBalance uses config service formatting")
    void testFormatBalance() {
        when(configService.formatAmount(500)).thenReturn("$500");

        String formatted = economyService.formatBalance(500);
        assertEquals("$500", formatted);
    }
}
