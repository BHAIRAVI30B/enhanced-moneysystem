package com.example.backend.unittest;

import com.example.backend.controllers.AccountController;
import com.example.backend.dtos.AccountDTO;
import com.example.backend.dtos.TransactionResponse;
import com.example.backend.exceptions.AccountNotFoundException;
import com.example.backend.security.service.UserDetailsImpl;
import com.example.backend.services.AccountService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountControllerTest {

    @InjectMocks
    private AccountController accountController;

    @Mock
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetAccount_admin_success() {

        AccountDTO account = new AccountDTO();
        account.setAccountId("123");
        account.setBalance(1000.0);

        when(accountService.getAccount("123")).thenReturn(account);

        ResponseEntity<?> response = accountController.getAccount("123");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(account, response.getBody());
    }

    @Test
    void testGetAccount_admin_notFound() {

        when(accountService.getAccount("999"))
                .thenThrow(new AccountNotFoundException("Account not found"));

        assertThrows(AccountNotFoundException.class,
                () -> accountController.getAccount("999"));
    }

    @Test
    void testGetTransactionsById_success() {

        TransactionResponse tx = new TransactionResponse();
        tx.setFromAccountId("123");
        tx.setToAccountId("456");
        tx.setAmount(100.0);

        List<TransactionResponse> transactions = List.of(tx);

        when(accountService.getTransactions("123")).thenReturn(transactions);

        ResponseEntity<List<TransactionResponse>> response =
                accountController.getTransactionsById("123");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(transactions, response.getBody());
    }

    @Test
    void testGetTransactionsById_notFound() {

        when(accountService.getTransactions("999"))
                .thenThrow(new AccountNotFoundException("Account not found"));

        assertThrows(AccountNotFoundException.class,
                () -> accountController.getTransactionsById("999"));
    }

    @Test
    void testGetMyAccount_success() {

        AccountDTO account = new AccountDTO();
        account.setAccountId("user123");
        account.setBalance(200.0);

        setUserContext("user123");

        when(accountService.getAccount("user123")).thenReturn(account);

        ResponseEntity<?> response = accountController.getMyAccount();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(account, response.getBody());
    }

    @Test
    void testGetMyAccount_notFound() {

        setUserContext("user999");

        when(accountService.getAccount("user999"))
                .thenThrow(new AccountNotFoundException("Account not found"));

        assertThrows(AccountNotFoundException.class,
                () -> accountController.getMyAccount());
    }

    @Test
    void testGetMyBalance_success() {

        AccountDTO account = new AccountDTO();
        account.setAccountId("user123");
        account.setBalance(300.0);

        setUserContext("user123");

        when(accountService.getAccount("user123")).thenReturn(account);

        ResponseEntity<?> response = accountController.getMyBalance();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(300.0, response.getBody());
    }

    @Test
    void testGetMyTransactions_success() {

        TransactionResponse tx = new TransactionResponse();
        tx.setFromAccountId("user123");
        tx.setToAccountId("456");
        tx.setAmount(50.0);

        List<TransactionResponse> transactions = List.of(tx);

        setUserContext("user123");

        when(accountService.getTransactions("user123")).thenReturn(transactions);

        ResponseEntity<List<TransactionResponse>> response =
                accountController.getMyTransactions();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(transactions, response.getBody());
    }

    @Test
    void testGetMyTransactions_notFound() {

        setUserContext("user999");

        when(accountService.getTransactions("user999"))
                .thenThrow(new AccountNotFoundException("Account not found"));

        assertThrows(AccountNotFoundException.class,
                () -> accountController.getMyTransactions());
    }

    @Test
    void testUpdateAccountStatus_success() {
        com.example.backend.dtos.AccountStatusUpdateRequest request =
                new com.example.backend.dtos.AccountStatusUpdateRequest();
        request.setStatus("LOCKED");

        AccountDTO updated = new AccountDTO();
        updated.setAccountId("123");
        updated.setStatus("LOCKED");

        when(accountService.updateStatus("123", "LOCKED")).thenReturn(updated);

        ResponseEntity<AccountDTO> response = accountController.updateAccountStatus("123", request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("LOCKED", response.getBody().getStatus());
    }

    @Test
    void testUpdateAccountStatus_accountNotFound_propagatesException() {
        com.example.backend.dtos.AccountStatusUpdateRequest request =
                new com.example.backend.dtos.AccountStatusUpdateRequest();
        request.setStatus("LOCKED");

        when(accountService.updateStatus("999", "LOCKED"))
                .thenThrow(new AccountNotFoundException("Account not found"));

        assertThrows(AccountNotFoundException.class,
                () -> accountController.updateAccountStatus("999", request));
    }

    @Test
    void testUpdateAccountStatus_invalidStatus_propagatesException() {
        com.example.backend.dtos.AccountStatusUpdateRequest request =
                new com.example.backend.dtos.AccountStatusUpdateRequest();
        request.setStatus("NOT_A_STATUS");

        when(accountService.updateStatus("123", "NOT_A_STATUS"))
                .thenThrow(new com.example.backend.exceptions.InvalidAccountStatusException(
                        "Invalid account status: NOT_A_STATUS"));

        assertThrows(com.example.backend.exceptions.InvalidAccountStatusException.class,
                () -> accountController.updateAccountStatus("123", request));
    }

    @Test
    void testGetMyRewards_success() {
        setUserContext("user123");

        com.example.backend.dtos.RewardResponse rewards =
                new com.example.backend.dtos.RewardResponse(5, java.util.List.of());

        when(accountService.getRewards("user123")).thenReturn(rewards);

        ResponseEntity<com.example.backend.dtos.RewardResponse> response =
                accountController.getMyRewards();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(5, response.getBody().getTotalPoints());
        assertTrue(response.getBody().getEntries().isEmpty());
    }

    @Test
    void testGetMyRewards_accountNotFound_propagatesException() {
        setUserContext("user999");

        when(accountService.getRewards("user999"))
                .thenThrow(new AccountNotFoundException("Account not found"));

        assertThrows(AccountNotFoundException.class,
                () -> accountController.getMyRewards());
    }

    private void setUserContext(String accountId) {

        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L,
                "user",
                "pass",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                accountId
        );

        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(userDetails, null));
    }
}