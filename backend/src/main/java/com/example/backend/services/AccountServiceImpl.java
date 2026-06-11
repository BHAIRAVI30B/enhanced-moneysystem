package com.example.backend.services;

import com.example.backend.dtos.AccountDTO;
import com.example.backend.entities.Account;
import com.example.backend.entities.TransactionLog;
import com.example.backend.dtos.TransactionResponse;
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
            TransactionResponse response = getTransactionResponse(transaction);

            transactionResponseList.add(response);
        }

        return transactionResponseList;
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
    public String generateAccountId() {
        return "ACC" + String.format("%04d", RANDOM.nextInt(10000));
    }
}
