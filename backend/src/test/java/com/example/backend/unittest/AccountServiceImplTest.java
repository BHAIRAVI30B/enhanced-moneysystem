package com.example.backend.unittest;

import com.example.backend.dtos.AccountDTO;
import com.example.backend.dtos.TransactionResponse;
import com.example.backend.entities.Account;
import com.example.backend.entities.TransactionLog;
import com.example.backend.enums.AccountStatus;
import com.example.backend.enums.TransactionStatus;
import com.example.backend.exceptions.AccountNotFoundException;
import com.example.backend.repositories.AccountRepository;
import com.example.backend.services.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountServiceImplTest {

    @InjectMocks
    private AccountServiceImpl accountService;

    @Mock
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
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
}
