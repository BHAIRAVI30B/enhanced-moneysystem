package com.example.backend.repositories;

import com.example.backend.entities.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, String> {
    TransactionLog findByIdempotencyKey(String idempotencyKey);
}
