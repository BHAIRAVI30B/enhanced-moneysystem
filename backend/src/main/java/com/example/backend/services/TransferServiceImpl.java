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

    // At most 10% of the bill amount can be covered using reward points.
    private static final double MAX_REDEMPTION_PERCENT = 0.10;

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final AccountService accountService;

    public TransferServiceImpl(AccountRepository accountRepository,
                               TransactionLogRepository transactionLogRepository,
                               AccountService accountService) {
        this.accountRepository = accountRepository;
        this.transactionLogRepository = transactionLogRepository;
        this.accountService = accountService;
    }

    @Override
    public TransactionResponse transfer(String fromAccountId,
                                        String toAccountId,
                                        Double amount,
                                        String idempotencyKey,
                                        String category,
                                        String note,
                                        Integer redeemPoints)
            throws AccountNotFoundException,
            AccountNotActiveException,
            InsufficientBalanceException,
            DuplicateTransferException,
            InvalidRedemptionException {

        TransactionStatus transactionStatus = TransactionStatus.SUCCESS;
        String failureReason = null;
        LocalDateTime now = LocalDateTime.now();

        Account fromAccount = accountRepository.findByAccountId(fromAccountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        String.format(SENDER_NOT_FOUND, fromAccountId)));

        Account toAccount = accountRepository.findByAccountId(toAccountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        String.format(RECEIVER_NOT_FOUND, toAccountId)));

        // Validate redemption request up front — a bad redemption request (e.g. more
        // points than available, or above the 10% cap) is a client error, not a failed
        // transaction, so it throws immediately rather than being logged as FAILED.
        int pointsToRedeem = validateAndResolveRedemption(fromAccountId, amount, redeemPoints);
        double discountAmount = pointsToRedeem; // 1 point = ₹1
        double amountToDebit = amount - discountAmount;

        try {
            validateAccounts(fromAccount, toAccount);
            validateBalance(fromAccount, amountToDebit);
            validateIdempotency(idempotencyKey);

            performTransfer(fromAccount, toAccount, amount, amountToDebit);

            if (pointsToRedeem > 0) {
                accountService.addRedeemedPoints(fromAccountId, pointsToRedeem);
            }

        } catch (AccountNotActiveException |
                 InsufficientBalanceException e) {

            transactionStatus = TransactionStatus.FAILED;
            failureReason = e.getMessage();
            // No money moves and no points are spent on a failed transaction.
            pointsToRedeem = 0;
            discountAmount = 0;
            amountToDebit = amount;
        }

        TransactionCategory categoryEnum = resolveCategory(category);

        TransactionLog log = buildTransactionLog(
                fromAccount, toAccount, amount,
                transactionStatus, failureReason,
                idempotencyKey, now,
                categoryEnum, note,
                pointsToRedeem, discountAmount
        );

        transactionLogRepository.save(log);

        return buildResponse(fromAccount, toAccount, amount,
                transactionStatus, failureReason, now,
                categoryEnum, note,
                pointsToRedeem, discountAmount, amountToDebit);
    }

    // Validates the requested redemption against eligibility rules and returns the
    // actual number of points to redeem (0 if redeemPoints is null/zero).
    // Throws InvalidRedemptionException if the request violates the rules outright.
    private int validateAndResolveRedemption(String fromAccountId, Double amount, Integer redeemPoints) {
        if (redeemPoints == null || redeemPoints <= 0) {
            return 0;
        }
        if (amount == null || amount <= 0) {
            throw new InvalidRedemptionException("Cannot redeem points on an invalid amount");
        }

        int availablePoints = accountService.getAvailablePoints(fromAccountId);
        int maxByCap = (int) Math.floor(amount * MAX_REDEMPTION_PERCENT);

        if (redeemPoints > availablePoints) {
            throw new InvalidRedemptionException(
                    "You only have " + availablePoints + " reward points available");
        }
        if (redeemPoints > maxByCap) {
            throw new InvalidRedemptionException(
                    "You can redeem at most " + maxByCap + " points (10% of the bill amount) for this transfer");
        }

        return redeemPoints;
    }

    private void validateAccounts(Account fromAccount, Account toAccount) {
        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(SENDER_NOT_ACTIVE);
        }
        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(RECEIVER_NOT_ACTIVE);
        }
    }

    private void validateBalance(Account fromAccount, Double amountToDebit) {
        if (fromAccount.getBalance() < amountToDebit) {
            throw new InsufficientBalanceException(INSUFFICIENT_BALANCE);
        }
    }

    private void validateIdempotency(String idempotencyKey) {
        if (transactionLogRepository.findByIdempotencyKey(idempotencyKey) != null) {
            throw new DuplicateTransferException(
                    String.format(DUPLICATE_TRANSFER, idempotencyKey));
        }
    }

    // Debits the discounted amount from the sender (amountToDebit), but always
    // credits the receiver the full bill amount — the sender's reward points
    // cover the gap, not the receiver's payout.
    private void performTransfer(Account fromAccount, Account toAccount, Double amount, Double amountToDebit) {
        fromAccount.setBalance(fromAccount.getBalance() - amountToDebit);
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
                                               String note,
                                               Integer pointsRedeemed,
                                               Double discountAmount) {

        TransactionLog log = new TransactionLog();
        log.setFromAccount(fromAccount);
        log.setToAccount(toAccount);
        log.setAmount(amount);
        log.setStatus(status);
        log.setFailureReason(failureReason);
        log.setIdempotencyKey(idempotencyKey);
        log.setCreatedOn(now);
        log.setPointsRedeemed(pointsRedeemed != null ? pointsRedeemed : 0);
        log.setDiscountAmount(discountAmount != null ? discountAmount : 0.0);

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
                                              String note,
                                              Integer pointsRedeemed,
                                              Double discountAmount,
                                              Double amountPaid) {

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
        response.setPointsRedeemed(pointsRedeemed != null ? pointsRedeemed : 0);
        response.setDiscountAmount(discountAmount != null ? discountAmount : 0.0);
        response.setAmountPaid(amountPaid != null ? amountPaid : amount);

        return response;
    }
}