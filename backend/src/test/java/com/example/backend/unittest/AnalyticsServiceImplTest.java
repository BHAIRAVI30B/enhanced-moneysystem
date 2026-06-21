package com.example.backend.unittest;

import com.example.backend.config.SnowflakeConfig;
import com.example.backend.dtos.AnalyticsResponse;
import com.example.backend.services.AnalyticsServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * AnalyticsServiceImpl talks to Snowflake via raw JDBC (Connection / PreparedStatement /
 * ResultSet) with no repository abstraction in between, so these are true unit tests of
 * "does the Java correctly react to whatever the JDBC layer returns" — they do NOT verify
 * the SQL text itself is correct against a real database. Every public method is covered
 * for both its success path and its SQLException/catch path.
 */
class AnalyticsServiceImplTest {

    private AnalyticsServiceImpl analyticsService;

    @Mock
    private SnowflakeConfig snowflakeConfig;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private static final String ACCOUNT_ID = "ACC1234";
    private static final long NUMERIC_ID = 7L;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        analyticsService = new AnalyticsServiceImpl(snowflakeConfig);
        when(snowflakeConfig.getConnection()).thenReturn(connection);
    }

    // ── small local matcher: "sql string contains this substring" ──
    private static String sqlContaining(String substring) {
        return argThat(new ArgumentMatcher<String>() {
            @Override
            public boolean matches(String argument) {
                return argument != null && argument.contains(substring);
            }

            @Override
            public String toString() {
                return "sql containing \"" + substring + "\"";
            }
        });
    }

    // resolveNumericId is private but exercised through every accountId-based method.
    // It issues its own PreparedStatement("SELECT ID FROM ACCOUNTS WHERE ACCOUNT_ID = ?")
    // BEFORE the main query's PreparedStatement — stub that exact SQL string separately
    // from the main query's statement so they don't collide.
    private void stubNumericIdLookupFound() throws SQLException {
        PreparedStatement idStmt = mock(PreparedStatement.class);
        ResultSet idRs = mock(ResultSet.class);
        when(connection.prepareStatement("SELECT ID FROM ACCOUNTS WHERE ACCOUNT_ID = ?"))
                .thenReturn(idStmt);
        when(idStmt.executeQuery()).thenReturn(idRs);
        when(idRs.next()).thenReturn(true);
        when(idRs.getLong("ID")).thenReturn(NUMERIC_ID);
    }

    private void stubNumericIdLookupNotFound() throws SQLException {
        PreparedStatement idStmt = mock(PreparedStatement.class);
        ResultSet idRs = mock(ResultSet.class);
        when(connection.prepareStatement("SELECT ID FROM ACCOUNTS WHERE ACCOUNT_ID = ?"))
                .thenReturn(idStmt);
        when(idStmt.executeQuery()).thenReturn(idRs);
        when(idRs.next()).thenReturn(false); // no row -> resolveNumericId throws SQLException
    }

    // ── getSentVsReceivedToday ────────────────────────────────────

    @Test
    void testGetSentVsReceivedToday_success() throws SQLException {
        stubNumericIdLookupFound();

        when(connection.prepareStatement(sqlContaining("TOTAL_SENT"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getDouble("TOTAL_SENT")).thenReturn(120.0);
        when(resultSet.getDouble("TOTAL_RECEIVED")).thenReturn(45.0);

        AnalyticsResponse.SentVsReceived result = analyticsService.getSentVsReceivedToday(ACCOUNT_ID);

        assertEquals(120.0, result.getTotalSent());
        assertEquals(45.0, result.getTotalReceived());
        verify(preparedStatement, times(1)).setLong(1, NUMERIC_ID);
        verify(preparedStatement, times(1)).setLong(2, NUMERIC_ID);
        verify(preparedStatement, times(1)).setLong(3, NUMERIC_ID);
        verify(preparedStatement, times(1)).setLong(4, NUMERIC_ID);
    }

    @Test
    void testGetSentVsReceivedToday_noRowsReturned_defaultsToZero() throws SQLException {
        stubNumericIdLookupFound();

        when(connection.prepareStatement(sqlContaining("TOTAL_SENT"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        AnalyticsResponse.SentVsReceived result = analyticsService.getSentVsReceivedToday(ACCOUNT_ID);

        assertEquals(0.0, result.getTotalSent());
        assertEquals(0.0, result.getTotalReceived());
    }

    @Test
    void testGetSentVsReceivedToday_accountNotFound_returnsZeroDefault() throws SQLException {
        stubNumericIdLookupNotFound();

        AnalyticsResponse.SentVsReceived result = analyticsService.getSentVsReceivedToday(ACCOUNT_ID);

        assertEquals(0.0, result.getTotalSent());
        assertEquals(0.0, result.getTotalReceived());
    }

    @Test
    void testGetSentVsReceivedToday_connectionThrows_returnsZeroDefault() throws SQLException {
        when(snowflakeConfig.getConnection()).thenThrow(new SQLException("connection refused"));

        AnalyticsResponse.SentVsReceived result = analyticsService.getSentVsReceivedToday(ACCOUNT_ID);

        assertEquals(0.0, result.getTotalSent());
        assertEquals(0.0, result.getTotalReceived());
    }

    // ── getUserStatusBreakdown (+ resolveDateFilter branches) ────────

    @Test
    void testGetUserStatusBreakdown_success_populatesList() throws SQLException {
        stubNumericIdLookupFound();

        when(connection.prepareStatement(sqlContaining("STATUS_LABEL"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("STATUS_LABEL")).thenReturn("SUCCESS", "FAILED");
        when(resultSet.getLong("TOTAL")).thenReturn(3L, 1L);

        List<AnalyticsResponse.StatusCount> result =
                analyticsService.getUserStatusBreakdown(ACCOUNT_ID, "day");

        assertEquals(2, result.size());
        assertEquals("SUCCESS", result.get(0).getStatus());
        assertEquals(3L, result.get(0).getCount());
        assertEquals("FAILED", result.get(1).getStatus());
        assertEquals(1L, result.get(1).getCount());
    }

    @Test
    void testGetUserStatusBreakdown_weekRange_buildsWeekFilter() throws SQLException {
        stubNumericIdLookupFound();
        when(connection.prepareStatement(sqlContaining("DATE_TRUNC('week'"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<AnalyticsResponse.StatusCount> result =
                analyticsService.getUserStatusBreakdown(ACCOUNT_ID, "week");

        assertTrue(result.isEmpty());
        verify(connection, times(1)).prepareStatement(sqlContaining("DATE_TRUNC('week'"));
    }

    @Test
    void testGetUserStatusBreakdown_monthRange_buildsMonthFilter() throws SQLException {
        stubNumericIdLookupFound();
        when(connection.prepareStatement(sqlContaining("DATE_TRUNC('month'"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        analyticsService.getUserStatusBreakdown(ACCOUNT_ID, "month");

        verify(connection, times(1)).prepareStatement(sqlContaining("DATE_TRUNC('month'"));
    }

    @Test
    void testGetUserStatusBreakdown_nullRange_defaultsToDayFilter() throws SQLException {
        stubNumericIdLookupFound();
        when(connection.prepareStatement(sqlContaining("CAST(CREATED_ON AS DATE) = CURRENT_DATE()")))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        analyticsService.getUserStatusBreakdown(ACCOUNT_ID, null);

        verify(connection, times(1))
                .prepareStatement(sqlContaining("CAST(CREATED_ON AS DATE) = CURRENT_DATE()"));
    }

    @Test
    void testGetUserStatusBreakdown_unrecognizedRange_defaultsToDayFilter() throws SQLException {
        stubNumericIdLookupFound();
        when(connection.prepareStatement(sqlContaining("CAST(CREATED_ON AS DATE) = CURRENT_DATE()")))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        analyticsService.getUserStatusBreakdown(ACCOUNT_ID, "not-a-real-range");

        verify(connection, times(1))
                .prepareStatement(sqlContaining("CAST(CREATED_ON AS DATE) = CURRENT_DATE()"));
    }

    @Test
    void testGetUserStatusBreakdown_rangeUppercaseWithWhitespace_isNormalized() throws SQLException {
        stubNumericIdLookupFound();
        when(connection.prepareStatement(sqlContaining("DATE_TRUNC('week'"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        analyticsService.getUserStatusBreakdown(ACCOUNT_ID, "  WEEK  ");

        verify(connection, times(1)).prepareStatement(sqlContaining("DATE_TRUNC('week'"));
    }

    @Test
    void testGetUserStatusBreakdown_sqlException_logsAndRethrowsAsRuntimeException() throws SQLException {
        when(snowflakeConfig.getConnection()).thenThrow(new SQLException("boom"));

        // Unlike most methods in this class (which swallow SQLException and return an
        // empty/default value), this one's catch block re-throws as RuntimeException.
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> analyticsService.getUserStatusBreakdown(ACCOUNT_ID, "day"));

        assertEquals("Failed to fetch user status breakdown", ex.getMessage());
        assertInstanceOf(SQLException.class, ex.getCause());
    }

    // ── getWeeklyExpenditureVsIncome ──────────────────────────────

    @Test
    void testGetWeeklyExpenditureVsIncome_success() throws SQLException {
        stubNumericIdLookupFound();
        when(connection.prepareStatement(sqlContaining("date_spine"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("DAY_LABEL")).thenReturn("01 Jan");
        when(resultSet.getDouble("EXPENDITURE")).thenReturn(200.0);
        when(resultSet.getDouble("INCOME")).thenReturn(50.0);

        List<AnalyticsResponse.DailyFlow> result =
                analyticsService.getWeeklyExpenditureVsIncome(ACCOUNT_ID);

        assertEquals(1, result.size());
        assertEquals("01 Jan", result.get(0).getDate());
        assertEquals(200.0, result.get(0).getExpenditure());
        assertEquals(50.0, result.get(0).getIncome());
    }

    @Test
    void testGetWeeklyExpenditureVsIncome_emptyResult() throws SQLException {
        stubNumericIdLookupFound();
        when(connection.prepareStatement(sqlContaining("date_spine"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<AnalyticsResponse.DailyFlow> result =
                analyticsService.getWeeklyExpenditureVsIncome(ACCOUNT_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetWeeklyExpenditureVsIncome_accountNotFound_returnsEmptyList() throws SQLException {
        stubNumericIdLookupNotFound();

        List<AnalyticsResponse.DailyFlow> result =
                analyticsService.getWeeklyExpenditureVsIncome(ACCOUNT_ID);

        assertTrue(result.isEmpty());
    }

    // ── getOverallStats ───────────────────────────────────────────

    @Test
    void testGetOverallStats_success() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getDouble("TOTAL_VOLUME")).thenReturn(5000.0);
        when(resultSet.getLong("TOTAL_TRANSACTIONS")).thenReturn(20L);
        when(resultSet.getLong("SUCCESSFUL")).thenReturn(18L);
        when(resultSet.getLong("FAILED")).thenReturn(2L);

        AnalyticsResponse.OverallStats result = analyticsService.getOverallStats();

        assertEquals(5000.0, result.getTotalVolume());
        assertEquals(20L, result.getTotalTransactions());
        assertEquals(18L, result.getSuccessful());
        assertEquals(2L, result.getFailed());
    }

    @Test
    void testGetOverallStats_noRows_returnsZeroDefault() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        AnalyticsResponse.OverallStats result = analyticsService.getOverallStats();

        assertEquals(0.0, result.getTotalVolume());
        assertEquals(0L, result.getTotalTransactions());
        assertEquals(0L, result.getSuccessful());
        assertEquals(0L, result.getFailed());
    }

    @Test
    void testGetOverallStats_sqlException_returnsZeroDefault() throws SQLException {
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("boom"));

        AnalyticsResponse.OverallStats result = analyticsService.getOverallStats();

        assertEquals(0.0, result.getTotalVolume());
        assertEquals(0L, result.getTotalTransactions());
    }

    @Test
    void testGetOverallStats_connectionThrows_returnsZeroDefault() throws SQLException {
        when(snowflakeConfig.getConnection()).thenThrow(new SQLException("connection refused"));

        AnalyticsResponse.OverallStats result = analyticsService.getOverallStats();

        assertEquals(0.0, result.getTotalVolume());
        assertEquals(0L, result.getTotalTransactions());
        assertEquals(0L, result.getSuccessful());
        assertEquals(0L, result.getFailed());
    }

    // ── getAdminStatusBreakdown ───────────────────────────────────

    @Test
    void testGetAdminStatusBreakdown_success() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, true, false);
        when(resultSet.getString("STATUS_LABEL")).thenReturn("Active", "Locked", "Closed");
        when(resultSet.getLong("TOTAL")).thenReturn(10L, 3L, 1L);

        List<AnalyticsResponse.StatusCount> result = analyticsService.getAdminStatusBreakdown();

        assertEquals(3, result.size());
        assertEquals("Active", result.get(0).getStatus());
        assertEquals(10L, result.get(0).getCount());
        assertEquals("Closed", result.get(2).getStatus());
        assertEquals(1L, result.get(2).getCount());
    }

    @Test
    void testGetAdminStatusBreakdown_sqlException_returnsEmptyList() throws SQLException {
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("boom"));

        List<AnalyticsResponse.StatusCount> result = analyticsService.getAdminStatusBreakdown();

        assertTrue(result.isEmpty());
    }

    // ── getTopSenders ─────────────────────────────────────────────

    @Test
    void testGetTopSenders_success() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("HOLDER_NAME")).thenReturn("John Doe");
        when(resultSet.getString("ACCOUNT_ID")).thenReturn(ACCOUNT_ID);
        when(resultSet.getDouble("TOTAL_SENT")).thenReturn(800.0);
        when(resultSet.getLong("TRANSACTION_COUNT")).thenReturn(6L);

        List<AnalyticsResponse.TopSender> result = analyticsService.getTopSenders();

        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getHolderName());
        assertEquals(ACCOUNT_ID, result.get(0).getAccountId());
        assertEquals(800.0, result.get(0).getTotalSent());
        assertEquals(6L, result.get(0).getTransactionCount());
    }

    @Test
    void testGetTopSenders_emptyResult() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<AnalyticsResponse.TopSender> result = analyticsService.getTopSenders();

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetTopSenders_sqlException_logsAndRethrowsAsRuntimeException() throws SQLException {
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("boom"));

        // Same as getUserStatusBreakdown — this catch block re-throws as RuntimeException
        // rather than swallowing it like the other admin methods.
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> analyticsService.getTopSenders());

        assertEquals("Failed to fetch top senders", ex.getMessage());
        assertInstanceOf(SQLException.class, ex.getCause());
    }

    // ── getCategoryBreakdown ──────────────────────────────────────

    @Test
    void testGetCategoryBreakdown_success() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("CATEGORY")).thenReturn("RENT", "GROCERY");
        when(resultSet.getLong("TOTAL")).thenReturn(4L, 9L);
        when(resultSet.getDouble("TOTAL_AMOUNT")).thenReturn(4000.0, 900.0);

        List<AnalyticsResponse.CategoryCount> result = analyticsService.getCategoryBreakdown();

        assertEquals(2, result.size());
        assertEquals("RENT", result.get(0).getCategory());
        assertEquals(4L, result.get(0).getCount());
        assertEquals(4000.0, result.get(0).getTotalAmount());
        assertEquals("GROCERY", result.get(1).getCategory());
    }

    @Test
    void testGetCategoryBreakdown_emptyResult() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<AnalyticsResponse.CategoryCount> result = analyticsService.getCategoryBreakdown();

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetCategoryBreakdown_sqlException_returnsEmptyList() throws SQLException {
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("boom"));

        List<AnalyticsResponse.CategoryCount> result = analyticsService.getCategoryBreakdown();

        assertTrue(result.isEmpty());
    }
}