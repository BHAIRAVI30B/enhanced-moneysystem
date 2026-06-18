package com.example.backend.controllers;

import com.example.backend.dtos.UserTransferRequest;
import com.example.backend.dtos.TransactionResponse;
import com.example.backend.exceptions.*;
import com.example.backend.security.service.UserDetailsImpl;
import com.example.backend.services.TransferService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/v1/transfers")
public class TransactionController {
    private final TransferService transferService;
    public TransactionController(TransferService transferService) {
        this.transferService = transferService;
    }

    // USER endpoint: only USER role can transfer (admins excluded)
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/user")
    public ResponseEntity<TransactionResponse> transferAsUser(
            @RequestBody UserTransferRequest request
    ) throws AccountNotFoundException, AccountNotActiveException,
            InsufficientBalanceException, DuplicateTransferException, InvalidRedemptionException {

        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String fromAccountId = userDetails.getAccountId(); // always user's own account

        TransactionResponse res = transferService.transfer(
                fromAccountId,
                request.getToAccountId(),
                request.getAmount(),
                request.getIdempotencyKey(),
                request.getCategory(),
                request.getNote(),
                request.getRedeemPoints()
        );
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }
}