package com.example.backend.entities;

import com.example.backend.enums.AccountStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String holderName;

    private Double balance;

    @Column(name = "account_id", unique = true, nullable = false, length = 7)
    private String accountId;

    private AccountStatus status;

    private Integer version;

    private LocalDateTime lastUpdated;

    // Outgoing transactions (this account is the sender)
    @OneToMany(mappedBy = "fromAccount", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<TransactionLog> outgoingTransactions;

    // Incoming transactions (this account is the receiver)
    @OneToMany(mappedBy = "toAccount", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<TransactionLog> incomingTransactions;

    // Getters
    public Long getId() {
        return id;
    }

    public String getHolderName() {
        return holderName;
    }

    public Double getBalance() {
        return balance;
    }

    public String getAccountId() {
        return accountId;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Integer getVersion() {
        return version;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public List<TransactionLog> getOutgoingTransactions() {
        return outgoingTransactions;
    }

    public List<TransactionLog> getIncomingTransactions() {
        return incomingTransactions;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setOutgoingTransactions(List<TransactionLog> outgoingTransactions) {
        this.outgoingTransactions = outgoingTransactions;
    }

    public void setIncomingTransactions(List<TransactionLog> incomingTransactions) {
        this.incomingTransactions = incomingTransactions;
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", holderName='" + holderName + '\'' +
                ", balance=" + balance +
                ", accountId='" + accountId + '\'' +
                ", status=" + status +
                ", version=" + version +
                ", lastUpdated=" + lastUpdated +
                ", outgoingTransactions=" + outgoingTransactions +
                ", incomingTransactions=" + incomingTransactions +
                '}';
    }
}
