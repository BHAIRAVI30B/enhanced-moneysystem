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
    AccountDTO updateStatus(String accountId, String status); // NEW
    int getAvailablePoints(String accountId); // NEW — total earned minus already redeemed
    void addRedeemedPoints(String accountId, int points); // NEW — records spent points
}