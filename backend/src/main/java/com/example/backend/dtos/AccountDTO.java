package com.example.backend.dtos;

import java.time.LocalDateTime;

public class AccountDTO {

    private Long id;
    private String holderName;
    private Double balance;
    private String accountId;
    private String status;
    private Integer version;
    private LocalDateTime lastUpdated;

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

    public String getStatus() {
        return status;
    }

    public Integer getVersion() {
        return version;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
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

    public void setStatus(String status) {
        this.status = status;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
