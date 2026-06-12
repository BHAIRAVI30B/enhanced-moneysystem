package com.example.backend.dtos;

import java.time.LocalDateTime;
import java.util.List;

public class RewardResponse {

    private int totalPoints;
    private List<RewardEntry> entries;

    public RewardResponse(int totalPoints, List<RewardEntry> entries) {
        this.totalPoints = totalPoints;
        this.entries = entries;
    }

    public int getTotalPoints() { return totalPoints; }
    public List<RewardEntry> getEntries() { return entries; }

    public static class RewardEntry {
        private String toAccountHolderName;
        private String toAccountId;
        private Double amount;
        private int points;
        private LocalDateTime createdOn;

        public RewardEntry(String toAccountHolderName, String toAccountId,
                           Double amount, int points, LocalDateTime createdOn) {
            this.toAccountHolderName = toAccountHolderName;
            this.toAccountId = toAccountId;
            this.amount = amount;
            this.points = points;
            this.createdOn = createdOn;
        }

        public String getToAccountHolderName() { return toAccountHolderName; }
        public String getToAccountId() { return toAccountId; }
        public Double getAmount() { return amount; }
        public int getPoints() { return points; }
        public LocalDateTime getCreatedOn() { return createdOn; }
    }
}