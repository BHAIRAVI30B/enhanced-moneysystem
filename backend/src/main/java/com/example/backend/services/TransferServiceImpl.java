package com.example.backend.services;

import com.example.backend.dtos.TransactionResponse;
import com.example.backend.entities.Account;
import com.example.backend.entities.TransactionDetails;
import com.example.backend.entities.TransactionLog;
import com.example.backend.enums.AccountStatus;
import com.example.backend.enums.TransactionCategory;
import com.example.backend.enums.TransactionStatus;
import com.example.backend.exceptions.*;
import com.example.backend.repositories.AccountRepository;
import com.example.backend.repositories.TransactionLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TransferServiceImpl implements TransferService {

    private static final String SENDER_NOT_FOUND = "Sender account not found: %s";
    private static final String RECEIVER_NOT_FOUND = "Receiver account not found: %s";
    private static final String SENDER_NOT_ACTIVE = "Sender account is not active";
    private static final String RECEIVER_NOT_ACTIVE = "Receiver account is not active";
    private static final String INSUFFICIENT_BALANCE = "Insufficient balance in sender account";
    private static final String DUPLICATE_TRANSFER = "Duplicate transfer detected with idempotency key: %s";

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;

    public TransferServiceImpl(AccountRepository accountRepository,
                               TransactionLogRepository transactionLogRepository) {
        this.accountRepository = accountRepository;
        this.transactionLogRepository = transactionLogRepository;
    }

    @Override
    public TransactionResponse transfer(String fromAccountId,
                                        String toAccountId,
                                        Double amount,
                                        String idempotencyKey,
                                        String category,
                                        String note)
            throws AccountNotFoundException,
            AccountNotActiveException,
            InsufficientBalanceException,
            DuplicateTransferException {

        TransactionStatus transactionStatus = TransactionStatus.SUCCESS;
        String failureReason = null;
        LocalDateTime now = LocalDateTime.now();

        Account fromAccount = accountRepository.findByAccountId(fromAccountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        String.format(SENDER_NOT_FOUND, fromAccountId)));

        Account toAccount = accountRepository.findByAccountId(toAccountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        String.format(RECEIVER_NOT_FOUND, toAccountId)));

        try {
            validateAccounts(fromAccount, toAccount);
            validateBalance(fromAccount, amount);
            validateIdempotency(idempotencyKey);

            performTransfer(fromAccount, toAccount, amount);

        } catch (AccountNotActiveException |
                 InsufficientBalanceException e) {

            transactionStatus = TransactionStatus.FAILED;
            failureReason = e.getMessage();
        }

        TransactionCategory categoryEnum = resolveCategory(category);

        TransactionLog log = buildTransactionLog(
                fromAccount, toAccount, amount,
                transactionStatus, failureReason,
                idempotencyKey, now,
                categoryEnum, note
        );

        transactionLogRepository.save(log);

        return buildResponse(fromAccount, toAccount, amount,
                transactionStatus, failureReason, now,
                categoryEnum, note);
    }

    private void validateAccounts(Account fromAccount, Account toAccount) {
        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(SENDER_NOT_ACTIVE);
        }
        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(RECEIVER_NOT_ACTIVE);
        }
    }

    private void validateBalance(Account fromAccount, Double amount) {
        if (fromAccount.getBalance() < amount) {
            throw new InsufficientBalanceException(INSUFFICIENT_BALANCE);
        }
    }

    private void validateIdempotency(String idempotencyKey) {
        if (transactionLogRepository.findByIdempotencyKey(idempotencyKey) != null) {
            throw new DuplicateTransferException(
                    String.format(DUPLICATE_TRANSFER, idempotencyKey));
        }
    }

    private void performTransfer(Account fromAccount, Account toAccount, Double amount) {
        fromAccount.setBalance(fromAccount.getBalance() - amount);
        toAccount.setBalance(toAccount.getBalance() + amount);

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
    }

    private TransactionCategory resolveCategory(String category) {
        if (category == null || category.isBlank()) {
            return TransactionCategory.OTHER;
        }
        try {
            return TransactionCategory.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return TransactionCategory.OTHER;
        }
    }

    private TransactionLog buildTransactionLog(Account fromAccount,
                                               Account toAccount,
                                               Double amount,
                                               TransactionStatus status,
                                               String failureReason,
                                               String idempotencyKey,
                                               LocalDateTime now,
                                               TransactionCategory category,
                                               String note) {

        TransactionLog log = new TransactionLog();
        log.setFromAccount(fromAccount);
        log.setToAccount(toAccount);
        log.setAmount(amount);
        log.setStatus(status);
        log.setFailureReason(failureReason);
        log.setIdempotencyKey(idempotencyKey);
        log.setCreatedOn(now);

        // attach details (separate table)
        TransactionDetails details = new TransactionDetails();
        details.setCategory(category);
        details.setNote(note);
        details.setTransactionLog(log);
        log.setDetails(details);

        return log;
    }

    private TransactionResponse buildResponse(Account fromAccount,
                                              Account toAccount,
                                              Double amount,
                                              TransactionStatus status,
                                              String failureReason,
                                              LocalDateTime now,
                                              TransactionCategory category,
                                              String note) {

        TransactionResponse response = new TransactionResponse();
        response.setFromAccountId(fromAccount.getAccountId());
        response.setFromAccountHolderName(fromAccount.getHolderName());
        response.setToAccountId(toAccount.getAccountId());
        response.setToAccountHolderName(toAccount.getHolderName());
        response.setAmount(amount);
        response.setStatus(status.name());
        response.setFailureReason(failureReason);
        response.setCreatedOn(now);
        response.setCategory(category != null ? category.name() : null);
        response.setNote(note);

        return response;
    }
}
