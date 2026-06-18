package com.example.backend.entities;

import com.example.backend.enums.TransactionCategory;
import jakarta.persistence.*;

@Entity
public class TransactionDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 50)
    private TransactionCategory category;

    @Column(nullable = true, length = 500)
    private String note;

    @OneToOne
    @JoinColumn(name = "transaction_log_id", nullable = false, unique = true)
    private TransactionLog transactionLog;

    // Getters
    public Long getId() {
        return id;
    }

    public TransactionCategory getCategory() {
        return category;
    }

    public String getNote() {
        return note;
    }

    public TransactionLog getTransactionLog() {
        return transactionLog;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setCategory(TransactionCategory category) {
        this.category = category;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setTransactionLog(TransactionLog transactionLog) {
        this.transactionLog = transactionLog;
    }
}