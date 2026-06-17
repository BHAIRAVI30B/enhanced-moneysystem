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
    @GetMapping("/user/sent-vs-received")
    public ResponseEntity<AnalyticsResponse.SentVsReceived> getSentVsReceived() {
        String accountId = getCurrentUser().getAccountId();
        return ResponseEntity.ok(analyticsService.getSentVsReceived(accountId));
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/user/status-breakdown")
    public ResponseEntity<List<AnalyticsResponse.StatusCount>> getUserStatusBreakdown() {
        String accountId = getCurrentUser().getAccountId();
        return ResponseEntity.ok(analyticsService.getUserStatusBreakdown(accountId));
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/user/reward-points")
    public ResponseEntity<List<AnalyticsResponse.RewardPoint>> getRewardPoints() {
        String accountId = getCurrentUser().getAccountId();
        return ResponseEntity.ok(analyticsService.getRewardPoints(accountId));
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

    private UserDetailsImpl getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (UserDetailsImpl) auth.getPrincipal();
    }
}