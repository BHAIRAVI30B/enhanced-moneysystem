package com.example.backend.controllers;

import com.example.backend.dtos.AccountDTO;
import com.example.backend.dtos.RewardResponse;
import com.example.backend.dtos.TransactionResponse;
import com.example.backend.exceptions.AccountNotFoundException;
import com.example.backend.security.service.UserDetailsImpl;
import com.example.backend.services.AccountService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<AccountDTO> getAccount(@PathVariable String id) {
        AccountDTO account = accountService.getAccount(id);
        return ResponseEntity.ok(account);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactionsById(
            @PathVariable String id) throws AccountNotFoundException {
        List<TransactionResponse> transactions = accountService.getTransactions(id);
        return ResponseEntity.ok(transactions);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/my-details")
    public ResponseEntity<AccountDTO> getMyAccount() {
        UserDetailsImpl userDetails = getCurrentUser();
        AccountDTO account = accountService.getAccount(userDetails.getAccountId());
        return ResponseEntity.ok(account);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/balance")
    public ResponseEntity<Double> getMyBalance() {
        UserDetailsImpl userDetails = getCurrentUser();
        AccountDTO account = accountService.getAccount(userDetails.getAccountId());
        return ResponseEntity.ok(account.getBalance());
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/my-transactions")
    public ResponseEntity<List<TransactionResponse>> getMyTransactions()
            throws AccountNotFoundException {
        UserDetailsImpl userDetails = getCurrentUser();
        List<TransactionResponse> transactions =
                accountService.getTransactions(userDetails.getAccountId());
        return ResponseEntity.ok(transactions);
    }

    // NEW: rewards endpoint
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/my-rewards")
    public ResponseEntity<RewardResponse> getMyRewards() {
        UserDetailsImpl userDetails = getCurrentUser();
        RewardResponse rewards = accountService.getRewards(userDetails.getAccountId());
        return ResponseEntity.ok(rewards);
    }

    private UserDetailsImpl getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logger.info("User: {}, Roles: {}", auth.getName(), auth.getAuthorities());
        return (UserDetailsImpl) auth.getPrincipal();
    }
}
