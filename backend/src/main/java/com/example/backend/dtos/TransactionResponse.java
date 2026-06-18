package com.example.backend.dtos;

import java.time.LocalDateTime;

public class TransactionResponse {

    private String fromAccountId;
    private String fromAccountHolderName;
    private String toAccountId;
    private String toAccountHolderName;
    private Double amount;
    private String status;
    private String failureReason;
    private String category;
    private String note;
    private LocalDateTime createdOn;

    // Reward redemption breakdown (0 / null-safe defaults when not used)
    private Integer pointsRedeemed = 0;
    private Double discountAmount = 0.0;
    private Double amountPaid; // amount actually debited from sender = amount - discountAmount

    // Getters
    public String getFromAccountId() {
        return fromAccountId;
    }

    public String getFromAccountHolderName() {
        return fromAccountHolderName;
    }

    public String getToAccountId() {
        return toAccountId;
    }

    public String getToAccountHolderName() {
        return toAccountHolderName;
    }

    public Double getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getCategory() {
        return category;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public Integer getPointsRedeemed() {
        return pointsRedeemed;
    }

    public Double getDiscountAmount() {
        return discountAmount;
    }

    public Double getAmountPaid() {
        return amountPaid;
    }

    // Setters
    public void setFromAccountId(String fromAccountId) {
        this.fromAccountId = fromAccountId;
    }

    public void setFromAccountHolderName(String fromAccountHolderName) {
        this.fromAccountHolderName = fromAccountHolderName;
    }

    public void setToAccountId(String toAccountId) {
        this.toAccountId = toAccountId;
    }

    public void setToAccountHolderName(String toAccountHolderName) {
        this.toAccountHolderName = toAccountHolderName;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }

    public void setPointsRedeemed(Integer pointsRedeemed) {
        this.pointsRedeemed = pointsRedeemed;
    }

    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public void setAmountPaid(Double amountPaid) {
        this.amountPaid = amountPaid;
    }

    @Override
    public String toString() {
        return "TransactionResponse{" +
                "fromAccountId='" + fromAccountId + '\'' +
                ", fromAccountHolderName='" + fromAccountHolderName + '\'' +
                ", toAccountId='" + toAccountId + '\'' +
                ", toAccountHolderName='" + toAccountHolderName + '\'' +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                ", failureReason='" + failureReason + '\'' +
                ", category='" + category + '\'' +
                ", note='" + note + '\'' +
                ", createdOn=" + createdOn +
                '}';
    }
}