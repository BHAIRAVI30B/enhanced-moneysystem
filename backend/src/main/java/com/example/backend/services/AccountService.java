package com.example.backend.services;

import com.example.backend.dtos.AccountDTO;
import com.example.backend.dtos.RewardResponse;
import com.example.backend.dtos.TransactionResponse;
import java.util.List;

public interface AccountService {
    AccountDTO getAccount(String id);
    Double getBalance(String id);
    List<TransactionResponse> getTransactions(String id);
    String generateAccountId();
    RewardResponse getRewards(String accountId); // NEW
}