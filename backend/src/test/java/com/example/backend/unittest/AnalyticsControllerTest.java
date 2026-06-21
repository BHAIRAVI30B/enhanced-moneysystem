package com.example.backend.unittest;

import com.example.backend.controllers.AnalyticsController;
import com.example.backend.dtos.AnalyticsResponse;
import com.example.backend.security.service.UserDetailsImpl;
import com.example.backend.services.AnalyticsService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AnalyticsControllerTest {

    @InjectMocks
    private AnalyticsController analyticsController;

    @Mock
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
    }

    // ── USER ENDPOINTS ───────────────────────────────────────────

    @Test
    void testGetSentVsReceivedToday_success() {
        setUserContext("ACC1234");

        AnalyticsResponse.SentVsReceived stats =
                new AnalyticsResponse.SentVsReceived(100.0, 50.0);
        when(analyticsService.getSentVsReceivedToday("ACC1234")).thenReturn(stats);

        ResponseEntity<AnalyticsResponse.SentVsReceived> response =
                analyticsController.getSentVsReceivedToday();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(stats, response.getBody());
        verify(analyticsService, times(1)).getSentVsReceivedToday("ACC1234");
    }

    @Test
    void testGetUserStatusBreakdown_withExplicitRange() {
        setUserContext("ACC1234");

        List<AnalyticsResponse.StatusCount> counts =
                List.of(new AnalyticsResponse.StatusCount("SUCCESS", 3));
        when(analyticsService.getUserStatusBreakdown("ACC1234", "week")).thenReturn(counts);

        ResponseEntity<List<AnalyticsResponse.StatusCount>> response =
                analyticsController.getUserStatusBreakdown("week");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(counts, response.getBody());
        verify(analyticsService, times(1)).getUserStatusBreakdown("ACC1234", "week");
    }

    @Test
    void testGetUserStatusBreakdown_defaultRange() {
        setUserContext("ACC1234");

        List<AnalyticsResponse.StatusCount> counts =
                List.of(new AnalyticsResponse.StatusCount("FAILED", 1));
        when(analyticsService.getUserStatusBreakdown("ACC1234", "day")).thenReturn(counts);

        // Simulates Spring not supplying the "range" query param —
        // @RequestParam default value "day" kicks in.
        ResponseEntity<List<AnalyticsResponse.StatusCount>> response =
                analyticsController.getUserStatusBreakdown("day");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(counts, response.getBody());
    }

    @Test
    void testGetWeeklyExpenditureVsIncome_success() {
        setUserContext("ACC1234");

        List<AnalyticsResponse.DailyFlow> flow =
                List.of(new AnalyticsResponse.DailyFlow("01 Jan", 200.0, 150.0));
        when(analyticsService.getWeeklyExpenditureVsIncome("ACC1234")).thenReturn(flow);

        ResponseEntity<List<AnalyticsResponse.DailyFlow>> response =
                analyticsController.getWeeklyExpenditureVsIncome();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(flow, response.getBody());
        verify(analyticsService, times(1)).getWeeklyExpenditureVsIncome("ACC1234");
    }

    // ── ADMIN ENDPOINTS ──────────────────────────────────────────

    @Test
    void testGetOverallStats_success() {
        AnalyticsResponse.OverallStats stats =
                new AnalyticsResponse.OverallStats(1000.0, 10, 8, 2);
        when(analyticsService.getOverallStats()).thenReturn(stats);

        ResponseEntity<AnalyticsResponse.OverallStats> response =
                analyticsController.getOverallStats();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(stats, response.getBody());
        verify(analyticsService, times(1)).getOverallStats();
    }

    @Test
    void testGetAdminStatusBreakdown_success() {
        List<AnalyticsResponse.StatusCount> counts =
                List.of(new AnalyticsResponse.StatusCount("Active", 7));
        when(analyticsService.getAdminStatusBreakdown()).thenReturn(counts);

        ResponseEntity<List<AnalyticsResponse.StatusCount>> response =
                analyticsController.getAdminStatusBreakdown();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(counts, response.getBody());
        verify(analyticsService, times(1)).getAdminStatusBreakdown();
    }

    @Test
    void testGetTopSenders_success() {
        List<AnalyticsResponse.TopSender> senders =
                List.of(new AnalyticsResponse.TopSender("John Doe", "ACC1234", 500.0, 4));
        when(analyticsService.getTopSenders()).thenReturn(senders);

        ResponseEntity<List<AnalyticsResponse.TopSender>> response =
                analyticsController.getTopSenders();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(senders, response.getBody());
        verify(analyticsService, times(1)).getTopSenders();
    }

    @Test
    void testGetCategoryBreakdown_success() {
        List<AnalyticsResponse.CategoryCount> categories =
                List.of(new AnalyticsResponse.CategoryCount("GROCERY", 5, 750.0));
        when(analyticsService.getCategoryBreakdown()).thenReturn(categories);

        ResponseEntity<List<AnalyticsResponse.CategoryCount>> response =
                analyticsController.getCategoryBreakdown();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(categories, response.getBody());
        verify(analyticsService, times(1)).getCategoryBreakdown();
    }

    private void setUserContext(String accountId) {
        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L,
                "user",
                "pass",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                accountId
        );

        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(userDetails, null));
    }
}