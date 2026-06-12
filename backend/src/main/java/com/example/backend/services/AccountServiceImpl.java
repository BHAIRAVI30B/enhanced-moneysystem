package com.example.backend.services;

import com.example.backend.dtos.AccountDTO;
import com.example.backend.dtos.RewardResponse;
import com.example.backend.dtos.TransactionResponse;
import com.example.backend.entities.Account;
import com.example.backend.entities.TransactionLog;
import com.example.backend.enums.AccountStatus;
import com.example.backend.enums.TransactionStatus;
import com.example.backend.exceptions.InvalidAccountStatusException;
import com.example.backend.repositories.AccountRepository;
import com.example.backend.exceptions.AccountNotFoundException;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class AccountServiceImpl implements AccountService {

    private static final Random RANDOM = new Random();
    private static final String ACCOUNT_NOT_FOUND_MSG = "Account with id %s not found";

    private final AccountRepository accountRepository;

    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public AccountDTO getAccount(String id) {
        Account account = accountRepository.findByAccountId(id)
                .orElseThrow(() -> new AccountNotFoundException(String.format(ACCOUNT_NOT_FOUND_MSG, id)));

        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setId(account.getId());
        accountDTO.setAccountId(account.getAccountId());
        accountDTO.setHolderName(account.getHolderName());
        accountDTO.setBalance(account.getBalance());
        accountDTO.setStatus(account.getStatus().name());
        accountDTO.setVersion(account.getVersion());
        accountDTO.setLastUpdated(account.getLastUpdated());

        return accountDTO;
    }

    @Override
    public Double getBalance(String id) {
        Account account = accountRepository.findByAccountId(id)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account with id " + id + " not found"));
        return account.getBalance();
    }

    @Override
    public List<TransactionResponse> getTransactions(String id) {
        Account account = accountRepository.findByAccountId(id)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account with id " + id + " not found"));

        List<TransactionLog> allTransactions = new ArrayList<>();
        allTransactions.addAll(account.getOutgoingTransactions());
        allTransactions.addAll(account.getIncomingTransactions());

        List<TransactionResponse> transactionResponseList = new ArrayList<>();
        for (TransactionLog transaction : allTransactions) {
            transactionResponseList.add(getTransactionResponse(transaction));
        }
        return transactionResponseList;
    }

    @Override
    public RewardResponse getRewards(String accountId) {
        Account account = accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        String.format(ACCOUNT_NOT_FOUND_MSG, accountId)));

        List<RewardResponse.RewardEntry> entries = new ArrayList<>();
        int totalPoints = 0;

        for (TransactionLog tx : account.getOutgoingTransactions()) {
            // Eligibility rules:
            // 1. Status must be SUCCESS
            // 2. Amount > 100
            // 3. Sender and receiver must be different accounts (no self-transfer)
            boolean isSuccess = TransactionStatus.SUCCESS.equals(tx.getStatus());
            boolean isAboveThreshold = tx.getAmount() != null && tx.getAmount() > 100;
            boolean isDifferentUser = !tx.getFromAccount().getAccountId()
                    .equals(tx.getToAccount().getAccountId());

            if (isSuccess && isAboveThreshold && isDifferentUser) {
                int points = (int) Math.floor(tx.getAmount() / 100); // 1 point per ₹100, rounded down
                totalPoints += points;

                entries.add(new RewardResponse.RewardEntry(
                        tx.getToAccount().getHolderName(),
                        tx.getToAccount().getAccountId(),
                        tx.getAmount(),
                        points,
                        tx.getCreatedOn()
                ));
            }
        }

        return new RewardResponse(totalPoints, entries);
    }

    private static @NonNull TransactionResponse getTransactionResponse(TransactionLog transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setFromAccountId(transaction.getFromAccount().getAccountId());
        response.setFromAccountHolderName(transaction.getFromAccount().getHolderName());
        response.setToAccountId(transaction.getToAccount().getAccountId());
        response.setToAccountHolderName(transaction.getToAccount().getHolderName());
        response.setAmount(transaction.getAmount());
        response.setStatus(transaction.getStatus().name());
        response.setCreatedOn(transaction.getCreatedOn());
        response.setFailureReason(transaction.getFailureReason());
        return response;
    }

    @Override
    public AccountDTO updateStatus(String accountId, String status) {
        Account account = accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new AccountNotFoundException(String.format(ACCOUNT_NOT_FOUND_MSG, accountId)));

        AccountStatus newStatus;
        try {
            newStatus = AccountStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidAccountStatusException(
                    "Invalid account status: " + status + ". Valid values are ACTIVE, LOCKED, CLOSED");
        }

        account.setStatus(newStatus);
        account.setLastUpdated(java.time.LocalDateTime.now());
        accountRepository.save(account);

        return getAccount(accountId);
    }

    @Override
    public String generateAccountId() {
        return "ACC" + String.format("%04d", RANDOM.nextInt(10000));
    }
}