package com.example.backend.services;


import com.example.backend.dtos.AccountDTO;
import com.example.backend.dtos.TransactionResponse;
import com.example.backend.exceptions.AccountNotFoundException;

import java.util.List;

public interface AccountService {
    AccountDTO getAccount(String id) throws AccountNotFoundException;
    Double getBalance(String id) throws AccountNotFoundException;
    List<TransactionResponse> getTransactions(String id) throws AccountNotFoundException;
    String generateAccountId();
}
