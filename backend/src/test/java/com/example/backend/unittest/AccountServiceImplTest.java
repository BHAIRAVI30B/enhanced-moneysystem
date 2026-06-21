package com.example.backend.unittest;

import com.example.backend.dtos.AccountDTO;
import com.example.backend.dtos.TransactionResponse;
import com.example.backend.entities.Account;
import com.example.backend.entities.TransactionLog;
import com.example.backend.enums.AccountStatus;
import com.example.backend.enums.TransactionStatus;
import com.example.backend.exceptions.AccountNotFoundException;
import com.example.backend.exceptions.InvalidAccountStatusException;
import com.example.backend.repositories.AccountRepository;
import com.example.backend.repositories.TransactionLogRepository;
import com.example.backend.services.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountServiceImplTest {

    private AccountServiceImpl accountService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        accountService = new AccountServiceImpl(accountRepository, transactionLogRepository);
    }

    @Test
    void testGetAccount_success() {
        // Arrange
        String accountId = "ACC1234";
        Account account = new Account();
        account.setId(1L);
        account.setAccountId(accountId);
        account.setHolderName("John Doe");
        account.setBalance(1000.0);
        account.setStatus(AccountStatus.ACTIVE);
        account.setVersion(1);
        account.setLastUpdated(LocalDateTime.now());

        when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.of(account));

        // Act
        AccountDTO result = accountService.getAccount(accountId);

        // Assert
        assertNotNull(result);
        assertEquals(account.getId(), result.getId());
        assertEquals(accountId, result.getAccountId());
        assertEquals(account.getId(), result.getId());
        assertEquals(account.getHolderName(), result.getHolderName());
        assertEquals(account.getBalance(), result.getBalance());
        assertEquals(account.getStatus().name(), result.getStatus());
    }

    @Test
    void testGetAccount_notFound() {
        // Arrange
        String accountId = "ACC1234";
        when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccountNotFoundException.class, () -> accountService.getAccount(accountId));
    }

    @Test
    void testGetBalance_success() {
        // Arrange
        String accountId = "ACC1234";
        Account account = new Account();
        account.setBalance(500.0);
        when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.of(account));

        // Act
        Double balance = accountService.getBalance(accountId);

        // Assert
        assertEquals(500.0, balance);
    }

    @Test
    void testGetBalance_notFound() {
        // Arrange
        String accountId = "ACC1234";
        when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccountNotFoundException.class, () -> accountService.getBalance(accountId));
    }

    @Test
    void testGetTransactions_success_populatesFields() {
        // Arrange
        String accountId = "ACC1234";
        Account account = new Account();
        account.setAccountId(accountId);
        account.setHolderName("John Doe");

        Account otherAccount = new Account();
        otherAccount.setAccountId("ACC5678");
        otherAccount.setHolderName("Jane Doe");

        TransactionLog outgoing = new TransactionLog();
        outgoing.setFromAccount(account);
        outgoing.setToAccount(otherAccount);
        outgoing.setAmount(100.0);
        outgoing.setStatus(TransactionStatus.SUCCESS);
        outgoing.setCreatedOn(LocalDateTime.now());

        TransactionLog incoming = new TransactionLog();
        incoming.setFromAccount(otherAccount);
        incoming.setToAccount(account);
        incoming.setAmount(50.0);
        incoming.setStatus(TransactionStatus.SUCCESS);
        incoming.setCreatedOn(LocalDateTime.now());

        account.setOutgoingTransactions(List.of(outgoing));
        account.setIncomingTransactions(List.of(incoming));

        when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.of(account));

        // Act
        List<TransactionResponse> responses = accountService.getTransactions(accountId);

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());

        TransactionResponse first = responses.get(0);
        assertEquals("ACC1234", first.getFromAccountId());
        assertEquals("John Doe", first.getFromAccountHolderName());
        assertEquals("ACC5678", first.getToAccountId());
        assertEquals("Jane Doe", first.getToAccountHolderName());
        assertEquals(100.0, first.getAmount());
        assertEquals(TransactionStatus.SUCCESS.name(), first.getStatus());
        assertNotNull(first.getCreatedOn());
    }

    @Test
    void testGetTransactions_notFound() {
        // Arrange
        String accountId = "ACC1234";
        when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccountNotFoundException.class, () -> accountService.getTransactions(accountId));
    }

    @Test
    void testGenerateAccountId() {
        // Act
        String accountId = accountService.generateAccountId();

        // Assert
        assertNotNull(accountId);
        assertTrue(accountId.startsWith("ACC"));
        assertEquals(7, accountId.length()); // "ACC" + 4 digits
    }

    // ── getRewards ──────────────────────────────────────────────────
    //
    // Eligibility filtering (SUCCESS status, amount > 100, not self-transfer)
    // now happens inside TransactionLogRepository.findEligibleRewardTransfers()
    // and sumEarnedPoints() — i.e. in the SQL query itself, not in this class.
    // These tests verify AccountServiceImpl correctly uses whatever the
    // repository returns; the query's own filtering logic belongs in a
    // TransactionLogRepository integration test, not here.

    private TransactionLog outgoingTx(Account from, Account to, Double amount,
                                      TransactionStatus status) {
        TransactionLog tx = new TransactionLog();
        tx.setFromAccount(from);
        tx.setToAccount(to);
        tx.setAmount(amount);
        tx.setStatus(status);
        tx.setCreatedOn(LocalDateTime.now());
        return tx;
    }

    @Test
    void testGetRewards_notFound() {
        when(accountRepository.findByAccountId("ACC9999")).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> accountService.getRewards("ACC9999"));
    }

    @Test
    void testGetRewards_buildsEntriesFromRepositoryResults() {
        String accountId = "ACC1234";
        Account self = new Account();
        self.setAccountId(accountId);
        self.setHolderName("John Doe");
        self.setRedeemedPoints(0);

        Account other = new Account();
        other.setAccountId("ACC5678");
        other.setHolderName("Jane Doe");

        // Repository already applied eligibility filtering — only qualifying
        // transfers are returned. floor(250/100) = 2 points.
        TransactionLog eligible = outgoingTx(self, other, 250.0, TransactionStatus.SUCCESS);

        when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.of(self));
        when(transactionLogRepository.findEligibleRewardTransfers(accountId))
                .thenReturn(List.of(eligible));

        var rewards = accountService.getRewards(accountId);

        assertEquals(2, rewards.getTotalPoints());
        assertEquals(1, rewards.getEntries().size());
        assertEquals("Jane Doe", rewards.getEntries().get(0).getToAccountHolderName());
        assertEquals("ACC5678", rewards.getEntries().get(0).getToAccountId());
        assertEquals(2, rewards.getEntries().get(0).getPoints());
    }

    @Test
    void testGetRewards_noEligibleTransfers_returnsZeroPointsAndEmptyEntries() {
        String accountId = "ACC1234";
        Account self = new Account();
        self.setAccountId(accountId);
        self.setRedeemedPoints(0);

        when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.of(self));
        when(transactionLogRepository.findEligibleRewardTransfers(accountId))
                .thenReturn(List.of());

        var rewards = accountService.getRewards(accountId);

        assertEquals(0, rewards.getTotalPoints());
        assertTrue(rewards.getEntries().isEmpty());
    }

    @Test
    void testGetRewards_subtractsAlreadyRedeemedPoints() {
        String accountId = "ACC1234";
        Account self = new Account();
        self.setAccountId(accountId);
        self.setRedeemedPoints(1); // already redeemed 1 of the 2 earned

        Account other = new Account();
        other.setAccountId("ACC5678");
        other.setHolderName("Jane Doe");

        TransactionLog eligible = outgoingTx(self, other, 250.0, TransactionStatus.SUCCESS); // 2 points

        when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.of(self));
        when(transactionLogRepository.findEligibleRewardTransfers(accountId))
                .thenReturn(List.of(eligible));

        var rewards = accountService.getRewards(accountId);

        assertEquals(1, rewards.getTotalPoints()); // 2 earned - 1 redeemed
    }

    @Test
    void testGetRewards_redeemedPointsNull_treatedAsZero() {
        String accountId = "ACC1234";
        Account self = new Account();
        self.setAccountId(accountId);
        self.setRedeemedPoints(null);

        Account other = new Account();
        other.setAccountId("ACC5678");
        other.setHolderName("Jane Doe");

        TransactionLog eligible = outgoingTx(self, other, 250.0, TransactionStatus.SUCCESS);

        when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.of(self));
        when(transactionLogRepository.findEligibleRewardTransfers(accountId))
                .thenReturn(List.of(eligible));

        var rewards = accountService.getRewards(accountId);

        assertEquals(2, rewards.getTotalPoints());
    }

    @Test
    void testGetRewards_redeemedExceedsEarned_clampedToZero() {
        String accountId = "ACC1234";
        Account self = new Account();
        self.setAccountId(accountId);
        self.setRedeemedPoints(100); // more than was ever earned

        Account other = new Account();
        other.setAccountId("ACC5678");
        other.setHolderName("Jane Doe");

        TransactionLog eligible = outgoingTx(self, other, 250.0, TransactionStatus.SUCCESS); // 2 points

        when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.of(self));
        when(transactionLogRepository.findEligibleRewardTransfers(accountId))
                .thenReturn(List.of(eligible));

        var rewards = accountService.getRewards(accountId);

        assertEquals(0, rewards.getTotalPoints()); // clamped at zero, never negative
    }

    // ── getAvailablePoints ──────────────────────────────────────────

    @Test
    void testGetAvailablePoints_notFound() {
        when(accountRepository.findByAccountId("ACC9999")).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> accountService.getAvailablePoints("ACC9999"));
    }

    @Test
    void testGetAvailablePoints_usesAggregateSumFromRepository() {
        String accountId = "ACC1234";
        Account self = new Account();
        self.setAccountId(accountId);
        self.setRedeemedPoints(3);

        when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.of(self));
        when(transactionLogRepository.sumEarnedPoints(accountId)).thenReturn(5);

        int available = accountService.getAvailablePoints(accountId);

        assertEquals(2, available); // 5 earned (from DB aggregate) - 3 redeemed
    }

    @Test
    void testGetAvailablePoints_redeemedPointsNull_treatedAsZero() {
        String accountId = "ACC1234";
        Account self = new Account();
        self.setAccountId(accountId);
        self.setRedeemedPoints(null);

        when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.of(self));
        when(transactionLogRepository.sumEarnedPoints(accountId)).thenReturn(5);

        int available = accountService.getAvailablePoints(accountId);

        assertEquals(5, available);
    }

    @Test
    void testGetAvailablePoints_redeemedExceedsEarned_clampedToZero() {
        String accountId = "ACC1234";
        Account self = new Account();
        self.setAccountId(accountId);
        self.setRedeemedPoints(50);

        when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.of(self));
        when(transactionLogRepository.sumEarnedPoints(accountId)).thenReturn(5);

        int available = accountService.getAvailablePoints(accountId);

        assertEquals(0, available);
    }

    // ── addRedeemedPoints ───────────────────────────────────────────

    @Test
    void testAddRedeemedPoints_zeroOrNegative_isNoOp() {
        accountService.addRedeemedPoints("ACC1234", 0);
        accountService.addRedeemedPoints("ACC1234", -5);

        verify(accountRepository, never()).findByAccountId(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void testAddRedeemedPoints_notFound() {
        when(accountRepository.findByAccountId("ACC9999")).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class,
                () -> accountService.addRedeemedPoints("ACC9999", 5));
    }

    @Test
    void testAddRedeemedPoints_accumulatesOnExistingValue() {
        String accountId = "ACC1234";
        Account account = new Account();
        account.setAccountId(accountId);
        account.setRedeemedPoints(4);

        when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.of(account));

        accountService.addRedeemedPoints(accountId, 6);

        assertEquals(10, account.getRedeemedPoints());
        verify(accountRepository, times(1)).save(account);
    }

    @Test
    void testAddRedeemedPoints_nullExistingValue_treatedAsZero() {
        String accountId = "ACC1234";
        Account account = new Account();
        account.setAccountId(accountId);
        account.setRedeemedPoints(null);

        when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.of(account));

        accountService.addRedeemedPoints(accountId, 6);

        assertEquals(6, account.getRedeemedPoints());
    }

    // ── updateStatus ──────────────────────────────────────────────

    @Test
    void testUpdateStatus_notFound() {
        when(accountRepository.findByAccountId("ACC9999")).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class,
                () -> accountService.updateStatus("ACC9999", "LOCKED"));
    }

    @Test
    void testUpdateStatus_validStatus_updatesAndReturnsDto() {
        String accountId = "ACC1234";
        Account account = new Account();
        account.setAccountId(accountId);
        account.setHolderName("John Doe");
        account.setBalance(1000.0);
        account.setStatus(AccountStatus.ACTIVE);
        account.setVersion(1);

        when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.of(account));

        AccountDTO result = accountService.updateStatus(accountId, "locked");

        assertEquals(AccountStatus.LOCKED, account.getStatus());
        assertNotNull(account.getLastUpdated());
        assertEquals("LOCKED", result.getStatus());
        verify(accountRepository, times(1)).save(account);
    }

    @Test
    void testUpdateStatus_invalidStatusString_throwsInvalidAccountStatusException() {
        String accountId = "ACC1234";
        Account account = new Account();
        account.setAccountId(accountId);

        when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.of(account));

        assertThrows(InvalidAccountStatusException.class,
                () -> accountService.updateStatus(accountId, "NOT_A_STATUS"));
    }

    @Test
    void testUpdateStatus_nullStatusString_throwsInvalidAccountStatusException() {
        String accountId = "ACC1234";
        Account account = new Account();
        account.setAccountId(accountId);

        when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.of(account));

        assertThrows(InvalidAccountStatusException.class,
                () -> accountService.updateStatus(accountId, null));
    }
}