package com.example.backend.repositories;

import com.example.backend.entities.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, String> {
    TransactionLog findByIdempotencyKey(String idempotencyKey);

    // Sums reward points (1 point per ₹100, rounded down) across all qualifying
    // outgoing transfers for an account. Eligibility: SUCCESS status, amount > 100,
    // not a self-transfer. Runs as a single DB aggregate instead of looping in Java —
    // this is on the hot path (called on every transfer to validate redemption).
    @Query("""
            SELECT COALESCE(SUM(FLOOR(t.amount / 100)), 0)
            FROM TransactionLog t
            WHERE t.fromAccount.accountId = :accountId
              AND t.status = com.example.backend.enums.TransactionStatus.SUCCESS
              AND t.amount > 100
              AND t.fromAccount.accountId <> t.toAccount.accountId
            """)
    int sumEarnedPoints(@Param("accountId") String accountId);

    // Returns the individual qualifying transfers (same eligibility rule as above),
    // most recent first — used to build the reward entry breakdown shown to the user.
    @Query("""
            SELECT t FROM TransactionLog t
            WHERE t.fromAccount.accountId = :accountId
              AND t.status = com.example.backend.enums.TransactionStatus.SUCCESS
              AND t.amount > 100
              AND t.fromAccount.accountId <> t.toAccount.accountId
            ORDER BY t.createdOn DESC
            """)
    List<TransactionLog> findEligibleRewardTransfers(@Param("accountId") String accountId);
}