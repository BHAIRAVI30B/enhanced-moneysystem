package com.example.backend.controllers;

import com.example.backend.dtos.AnalyticsResponse;
import com.example.backend.security.service.UserDetailsImpl;
import com.example.backend.services.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // ── USER ENDPOINTS ───────────────────────────────────────────

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/user/sent-vs-received-today")
    public ResponseEntity<AnalyticsResponse.SentVsReceived> getSentVsReceivedToday() {
        String accountId = getCurrentUser().getAccountId();
        return ResponseEntity.ok(analyticsService.getSentVsReceivedToday(accountId));
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/user/status-breakdown")
    public ResponseEntity<List<AnalyticsResponse.StatusCount>> getUserStatusBreakdown(
            @RequestParam(name = "range", defaultValue = "day") String range) {
        String accountId = getCurrentUser().getAccountId();
        return ResponseEntity.ok(analyticsService.getUserStatusBreakdown(accountId, range));
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/user/weekly-flow")
    public ResponseEntity<List<AnalyticsResponse.DailyFlow>> getWeeklyExpenditureVsIncome() {
        String accountId = getCurrentUser().getAccountId();
        return ResponseEntity.ok(analyticsService.getWeeklyExpenditureVsIncome(accountId));
    }

    // ── ADMIN ENDPOINTS ──────────────────────────────────────────

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/overall-stats")
    public ResponseEntity<AnalyticsResponse.OverallStats> getOverallStats() {
        return ResponseEntity.ok(analyticsService.getOverallStats());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/status-breakdown")
    public ResponseEntity<List<AnalyticsResponse.StatusCount>> getAdminStatusBreakdown() {
        return ResponseEntity.ok(analyticsService.getAdminStatusBreakdown());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/top-senders")
    public ResponseEntity<List<AnalyticsResponse.TopSender>> getTopSenders() {
        return ResponseEntity.ok(analyticsService.getTopSenders());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/category-breakdown")
    public ResponseEntity<List<AnalyticsResponse.CategoryCount>> getCategoryBreakdown() {
        return ResponseEntity.ok(analyticsService.getCategoryBreakdown());
    }

    private UserDetailsImpl getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (UserDetailsImpl) auth.getPrincipal();
    }
}