package com.example.backend.services;


import com.example.backend.dtos.TransactionResponse;
import com.example.backend.exceptions.*;

public interface TransferService {
    TransactionResponse transfer(String fromAccountId,
                                 String toAccountId,
                                 Double amount,
                                 String idempotencyKey,
                                 String category,
                                 String note,
                                 Integer redeemPoints)
            throws AccountNotFoundException, AccountNotActiveException,
            InsufficientBalanceException, DuplicateTransferException, InvalidRedemptionException;
}