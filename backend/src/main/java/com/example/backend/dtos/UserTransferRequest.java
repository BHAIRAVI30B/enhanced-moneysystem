package com.example.backend.dtos;

public class UserTransferRequest {

    private String toAccountId;
    private Double amount;
    private String idempotencyKey;

    private String category;

    private String note;

    // Optional: number of reward points the user wants to redeem against this transfer.
    // 1 point = ₹1 discount, capped at 10% of the bill amount and at available points.
    private Integer redeemPoints;

    // Getters
    public String getToAccountId() {
        return toAccountId;
    }

    public Double getAmount() {
        return amount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getCategory() {
        return category;
    }

    public String getNote() {
        return note;
    }

    public Integer getRedeemPoints() {
        return redeemPoints;
    }

    // Setters
    public void setToAccountId(String toAccountId) {
        this.toAccountId = toAccountId;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setRedeemPoints(Integer redeemPoints) {
        this.redeemPoints = redeemPoints;
    }
}