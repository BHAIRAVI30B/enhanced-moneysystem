package com.example.backend.services;

import com.example.backend.dtos.AnalyticsResponse;

import java.util.List;

public interface AnalyticsService {
    // User
    AnalyticsResponse.SentVsReceived getSentVsReceivedToday(String accountId);
    List<AnalyticsResponse.StatusCount> getUserStatusBreakdown(String accountId, String range);
    List<AnalyticsResponse.DailyFlow> getWeeklyExpenditureVsIncome(String accountId);

    // Admin
    AnalyticsResponse.OverallStats getOverallStats();
    List<AnalyticsResponse.StatusCount> getAdminStatusBreakdown();
    List<AnalyticsResponse.TopSender> getTopSenders();
    List<AnalyticsResponse.CategoryCount> getCategoryBreakdown();
}