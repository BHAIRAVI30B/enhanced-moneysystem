package com.example.backend.unittest;

import com.example.backend.controllers.TransactionController;
import com.example.backend.dtos.UserTransferRequest;
import com.example.backend.dtos.TransactionResponse;
import com.example.backend.enums.TransactionStatus;
import com.example.backend.exceptions.*;
import com.example.backend.security.service.UserDetailsImpl;
import com.example.backend.services.TransferService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionControllerTest {

    @InjectMocks
    private TransactionController transactionController;

    @Mock
    private TransferService transferService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
    }

    @Test
    void testTransfer_user_success() throws Exception {

        setUserContext();

        UserTransferRequest request = new UserTransferRequest();
        request.setToAccountId("456");
        request.setAmount(100.0);
        request.setIdempotencyKey("key123");
        request.setCategory("RENT");
        request.setNote("Paying monthly rent");

        TransactionResponse txResponse = new TransactionResponse();
        txResponse.setFromAccountId("user123");
        txResponse.setToAccountId("456");
        txResponse.setAmount(100.0);
        txResponse.setStatus(TransactionStatus.SUCCESS.name());
        txResponse.setCategory("RENT");
        txResponse.setNote("Paying monthly rent");

        when(transferService.transfer("user123", "456", 100.0, "key123", "RENT", "Paying monthly rent", null))
                .thenReturn(txResponse);

        ResponseEntity<TransactionResponse> response =
                transactionController.transferAsUser(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(txResponse, response.getBody());
    }

    @Test
    void testTransfer_user_accountNotFound() throws Exception {

        setUserContext();

        UserTransferRequest request = new UserTransferRequest();
        request.setToAccountId("999");
        request.setAmount(100.0);
        request.setIdempotencyKey("key123");

        when(transferService.transfer("user123", "999", 100.0, "key123", null, null, null))
                .thenThrow(new AccountNotFoundException("Receiver account not found"));

        assertThrows(AccountNotFoundException.class,
                () -> transactionController.transferAsUser(request));
    }

    @Test
    void testTransfer_user_accountNotActive() throws Exception {

        setUserContext();

        UserTransferRequest request = new UserTransferRequest();
        request.setToAccountId("456");
        request.setAmount(100.0);
        request.setIdempotencyKey("key123");

        when(transferService.transfer("user123", "456", 100.0, "key123", null, null, null))
                .thenThrow(new AccountNotActiveException("Sender account is not active"));

        assertThrows(AccountNotActiveException.class,
                () -> transactionController.transferAsUser(request));
    }

    @Test
    void testTransfer_user_insufficientBalance() throws Exception {

        setUserContext();

        UserTransferRequest request = new UserTransferRequest();
        request.setToAccountId("456");
        request.setAmount(1000.0);
        request.setIdempotencyKey("key123");

        when(transferService.transfer("user123", "456", 1000.0, "key123", null, null, null))
                .thenThrow(new InsufficientBalanceException("Insufficient balance"));

        assertThrows(InsufficientBalanceException.class,
                () -> transactionController.transferAsUser(request));
    }

    @Test
    void testTransfer_user_duplicateTransfer() throws Exception {

        setUserContext();

        UserTransferRequest request = new UserTransferRequest();
        request.setToAccountId("456");
        request.setAmount(100.0);
        request.setIdempotencyKey("dupKey");

        when(transferService.transfer("user123", "456", 100.0, "dupKey", null, null, null))
                .thenThrow(new DuplicateTransferException("Duplicate transfer detected"));

        assertThrows(DuplicateTransferException.class,
                () -> transactionController.transferAsUser(request));
    }

    private void setUserContext() {

        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L,
                "user",
                "pass",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                "user123"
        );

        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(userDetails, null));
    }
}