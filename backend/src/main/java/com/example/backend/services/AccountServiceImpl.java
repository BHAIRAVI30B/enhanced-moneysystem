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
import com.example.backend.repositories.TransactionLogRepository;
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
    private final TransactionLogRepository transactionLogRepository;

    public AccountServiceImpl(AccountRepository accountRepository,
                              TransactionLogRepository transactionLogRepository) {
        this.accountRepository = accountRepository;
        this.transactionLogRepository = transactionLogRepository;
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
        // Failed transfers never actually moved money, so they should not
        // appear in the receiver's history — only the sender attempted them.
        for (TransactionLog tx : account.getIncomingTransactions()) {
            if (tx.getStatus() != TransactionStatus.FAILED) {
                allTransactions.add(tx);
            }
        }

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

        List<TransactionLog> eligible = transactionLogRepository.findEligibleRewardTransfers(accountId);

        List<RewardResponse.RewardEntry> entries = new ArrayList<>();
        int totalEarned = 0;
        for (TransactionLog tx : eligible) {
            int points = (int) Math.floor(tx.getAmount() / 100); // 1 point per ₹100, rounded down
            totalEarned += points;
            entries.add(new RewardResponse.RewardEntry(
                    tx.getToAccount().getHolderName(),
                    tx.getToAccount().getAccountId(),
                    tx.getAmount(),
                    points,
                    tx.getCreatedOn()
            ));
        }

        int redeemed = account.getRedeemedPoints() != null ? account.getRedeemedPoints() : 0;
        int available = Math.max(0, totalEarned - redeemed);

        return new RewardResponse(available, entries);
    }

    @Override
    public int getAvailablePoints(String accountId) {
        Account account = accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        String.format(ACCOUNT_NOT_FOUND_MSG, accountId)));

        // Single DB aggregate instead of loading every transaction into memory —
        // this method runs on every transfer to validate redemption, so it's the hot path.
        int totalEarned = transactionLogRepository.sumEarnedPoints(accountId);
        int redeemed = account.getRedeemedPoints() != null ? account.getRedeemedPoints() : 0;
        return Math.max(0, totalEarned - redeemed);
    }

    @Override
    public void addRedeemedPoints(String accountId, int points) {
        if (points <= 0) {
            return;
        }
        Account account = accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        String.format(ACCOUNT_NOT_FOUND_MSG, accountId)));

        int current = account.getRedeemedPoints() != null ? account.getRedeemedPoints() : 0;
        account.setRedeemedPoints(current + points);
        accountRepository.save(account);
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