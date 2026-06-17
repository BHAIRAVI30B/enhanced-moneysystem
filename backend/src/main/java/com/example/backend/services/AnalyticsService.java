package com.example.backend.services;

import com.example.backend.dtos.AnalyticsResponse;

import java.util.List;

public interface AnalyticsService {
    // User
    AnalyticsResponse.SentVsReceived getSentVsReceived(String accountId);
    List<AnalyticsResponse.StatusCount> getUserStatusBreakdown(String accountId);
    List<AnalyticsResponse.RewardPoint> getRewardPoints(String accountId);

    // Admin
    AnalyticsResponse.OverallStats getOverallStats();
    List<AnalyticsResponse.StatusCount> getAdminStatusBreakdown();
    List<AnalyticsResponse.TopSender> getTopSenders();
}