package com.example.backend.dtos;

import java.util.List;
import java.util.Map;

public class AnalyticsResponse {

    // ── USER ANALYTICS ──────────────────────────────────────────

    // Chart 1: Sent vs Received
    public static class SentVsReceived {
        private double totalSent;
        private double totalReceived;

        public SentVsReceived(double totalSent, double totalReceived) {
            this.totalSent = totalSent;
            this.totalReceived = totalReceived;
        }

        public double getTotalSent() { return totalSent; }
        public double getTotalReceived() { return totalReceived; }
    }

    // Chart 2: Transaction status breakdown (user)
    public static class StatusCount {
        private String status;
        private long count;

        public StatusCount(String status, long count) {
            this.status = status;
            this.count = count;
        }

        public String getStatus() { return status; }
        public long getCount() { return count; }
    }

    // Chart 3: Reward points per transaction (bar)
    public static class RewardPoint {
        private String receiver;
        private double amount;
        private int points;
        private String date;

        public RewardPoint(String receiver, double amount, int points, String date) {
            this.receiver = receiver;
            this.amount = amount;
            this.points = points;
            this.date = date;
        }

        public String getReceiver() { return receiver; }
        public double getAmount() { return amount; }
        public int getPoints() { return points; }
        public String getDate() { return date; }
    }

    // ── ADMIN ANALYTICS ─────────────────────────────────────────

    // Chart 4: Overall stats
    public static class OverallStats {
        private double totalVolume;
        private long totalTransactions;
        private long successful;
        private long failed;

        public OverallStats(double totalVolume, long totalTransactions, long successful, long failed) {
            this.totalVolume = totalVolume;
            this.totalTransactions = totalTransactions;
            this.successful = successful;
            this.failed = failed;
        }

        public double getTotalVolume() { return totalVolume; }
        public long getTotalTransactions() { return totalTransactions; }
        public long getSuccessful() { return successful; }
        public long getFailed() { return failed; }
    }

    // Chart 5: Status breakdown (admin — all users)
    // reuses StatusCount

    // Chart 6: Top senders
    public static class TopSender {
        private String holderName;
        private String accountId;
        private double totalSent;
        private long transactionCount;

        public TopSender(String holderName, String accountId, double totalSent, long transactionCount) {
            this.holderName = holderName;
            this.accountId = accountId;
            this.totalSent = totalSent;
            this.transactionCount = transactionCount;
        }

        public String getHolderName() { return holderName; }
        public String getAccountId() { return accountId; }
        public double getTotalSent() { return totalSent; }
        public long getTransactionCount() { return transactionCount; }
    }
}