package com.example.backend.services;

import com.example.backend.config.SnowflakeConfig;
import com.example.backend.dtos.AnalyticsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsServiceImpl.class);
    private final SnowflakeConfig snowflakeConfig;

    // Transaction status enums (as stored in Snowflake)
    private static final int TX_SUCCESS = 0;
    private static final int TX_FAILED  = 1;

    // Account status enums (as stored in Snowflake)
    private static final int ACC_ACTIVE = 0;
    private static final int ACC_LOCK   = 1;
    private static final int ACC_CLOSED = 2;

    public AnalyticsServiceImpl(SnowflakeConfig snowflakeConfig) {
        this.snowflakeConfig = snowflakeConfig;
    }

    // Resolves string accountId (e.g. ACC9878) to numeric ID from Snowflake ACCOUNTS table
    private long resolveNumericId(Connection conn, String accountId) throws SQLException {
        String sql = "SELECT ID FROM ACCOUNTS WHERE ACCOUNT_ID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getLong("ID");
        }
        throw new SQLException("Account not found in Snowflake: " + accountId);
    }

    // ── USER ANALYTICS ───────────────────────────────────────────

    @Override
    public AnalyticsResponse.SentVsReceived getSentVsReceivedToday(String accountId) {
        // STATUS = 0 means SUCCESS; restricted to today only
        String sql = """
                SELECT
                    COALESCE(SUM(CASE WHEN FROM_ACCOUNT_ID = ? THEN AMOUNT ELSE 0 END), 0) AS TOTAL_SENT,
                    COALESCE(SUM(CASE WHEN TO_ACCOUNT_ID   = ? THEN AMOUNT ELSE 0 END), 0) AS TOTAL_RECEIVED
                FROM TRANSACTION_LOG
                WHERE STATUS = 0
                  AND (FROM_ACCOUNT_ID = ? OR TO_ACCOUNT_ID = ?)
                  AND CAST(CREATED_ON AS DATE) = CURRENT_DATE()
                """;

        try (Connection conn = snowflakeConfig.getConnection()) {
            long numericId = resolveNumericId(conn, accountId);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, numericId);
                stmt.setLong(2, numericId);
                stmt.setLong(3, numericId);
                stmt.setLong(4, numericId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return new AnalyticsResponse.SentVsReceived(
                            rs.getDouble("TOTAL_SENT"),
                            rs.getDouble("TOTAL_RECEIVED")
                    );
                }
            }
        } catch (SQLException e) {
            logger.error("Snowflake error in getSentVsReceivedToday: {}", e.getMessage());
        }
        return new AnalyticsResponse.SentVsReceived(0, 0);
    }

    @Override
    public List<AnalyticsResponse.StatusCount> getUserStatusBreakdown(String accountId, String range) {
        String dateFilter = resolveDateFilter(range);

        // Map numeric status to readable labels
        String sql = """
                SELECT
                    CASE STATUS
                        WHEN 0 THEN 'SUCCESS'
                        WHEN 1 THEN 'FAILED'
                        ELSE 'UNKNOWN'
                    END AS STATUS_LABEL,
                    COUNT(*) AS TOTAL
                FROM TRANSACTION_LOG
                WHERE FROM_ACCOUNT_ID = ?
                """ + "AND " + dateFilter + """
                GROUP BY STATUS
                """;

        List<AnalyticsResponse.StatusCount> result = new ArrayList<>();
        try (Connection conn = snowflakeConfig.getConnection()) {
            long numericId = resolveNumericId(conn, accountId);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, numericId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    result.add(new AnalyticsResponse.StatusCount(
                            rs.getString("STATUS_LABEL"),
                            rs.getLong("TOTAL")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Snowflake error in getUserStatusBreakdown: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch user status breakdown", e);
        }
        return result;
    }

    // Builds a SQL boolean expression filtering CREATED_ON by day/week/month.
    // Defaults to day if range is null/unrecognized.
    private String resolveDateFilter(String range) {
        String normalized = range == null ? "day" : range.trim().toLowerCase();
        return switch (normalized) {
            case "week"  -> "CREATED_ON >= DATE_TRUNC('week', CURRENT_DATE())";
            case "month" -> "CREATED_ON >= DATE_TRUNC('month', CURRENT_DATE())";
            default      -> "CAST(CREATED_ON AS DATE) = CURRENT_DATE()";
        };
    }

    @Override
    public List<AnalyticsResponse.DailyFlow> getWeeklyExpenditureVsIncome(String accountId) {
        // Last 7 days (including today), STATUS = 0 SUCCESS only.
        // Expenditure = money sent out, Income = money received in.
        String sql = """
                WITH date_spine AS (
                    SELECT DATEADD('day', -SEQ4(), CURRENT_DATE()) AS DAY
                    FROM TABLE(GENERATOR(ROWCOUNT => 7))
                ),
                daily AS (
                    SELECT
                        CAST(CREATED_ON AS DATE) AS DAY,
                        SUM(CASE WHEN FROM_ACCOUNT_ID = ? THEN AMOUNT ELSE 0 END) AS EXPENDITURE,
                        SUM(CASE WHEN TO_ACCOUNT_ID   = ? THEN AMOUNT ELSE 0 END) AS INCOME
                    FROM TRANSACTION_LOG
                    WHERE STATUS = 0
                      AND (FROM_ACCOUNT_ID = ? OR TO_ACCOUNT_ID = ?)
                      AND CREATED_ON >= DATEADD('day', -6, CURRENT_DATE())
                    GROUP BY CAST(CREATED_ON AS DATE)
                )
                SELECT
                    TO_CHAR(ds.DAY, 'DD Mon')                AS DAY_LABEL,
                    COALESCE(d.EXPENDITURE, 0)                AS EXPENDITURE,
                    COALESCE(d.INCOME, 0)                     AS INCOME
                FROM date_spine ds
                LEFT JOIN daily d ON ds.DAY = d.DAY
                ORDER BY ds.DAY ASC
                """;

        List<AnalyticsResponse.DailyFlow> result = new ArrayList<>();
        try (Connection conn = snowflakeConfig.getConnection()) {
            long numericId = resolveNumericId(conn, accountId);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, numericId);
                stmt.setLong(2, numericId);
                stmt.setLong(3, numericId);
                stmt.setLong(4, numericId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    result.add(new AnalyticsResponse.DailyFlow(
                            rs.getString("DAY_LABEL"),
                            rs.getDouble("EXPENDITURE"),
                            rs.getDouble("INCOME")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Snowflake error in getWeeklyExpenditureVsIncome: {}", e.getMessage());
        }
        return result;
    }

    // ── ADMIN ANALYTICS ──────────────────────────────────────────

    @Override
    public AnalyticsResponse.OverallStats getOverallStats() {
        // STATUS = 0 SUCCESS, STATUS = 1 FAILED
        String sql = """
                SELECT
                    COALESCE(SUM(AMOUNT), 0)                            AS TOTAL_VOLUME,
                    COUNT(*)                                             AS TOTAL_TRANSACTIONS,
                    COUNT(CASE WHEN STATUS = 0 THEN 1 END)              AS SUCCESSFUL,
                    COUNT(CASE WHEN STATUS = 1 THEN 1 END)              AS FAILED
                FROM TRANSACTION_LOG
                """;

        try (Connection conn = snowflakeConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return new AnalyticsResponse.OverallStats(
                        rs.getDouble("TOTAL_VOLUME"),
                        rs.getLong("TOTAL_TRANSACTIONS"),
                        rs.getLong("SUCCESSFUL"),
                        rs.getLong("FAILED")
                );
            }
        } catch (SQLException e) {
            logger.error("Snowflake error in getOverallStats: {}", e.getMessage());
        }
        return new AnalyticsResponse.OverallStats(0, 0, 0, 0);
    }

    @Override
    public List<AnalyticsResponse.StatusCount> getAdminStatusBreakdown() {
        // Changed: now shows ACCOUNT STATUS breakdown (Active/Locked/Closed)
        // instead of transaction status — since overall stats already covers transactions
        String sql = """
                SELECT
                    CASE STATUS
                        WHEN 0 THEN 'Active'
                        WHEN 1 THEN 'Locked'
                        WHEN 2 THEN 'Closed'
                        ELSE 'Unknown'
                    END AS STATUS_LABEL,
                    COUNT(*) AS TOTAL
                FROM ACCOUNTS
                GROUP BY STATUS
                """;

        List<AnalyticsResponse.StatusCount> result = new ArrayList<>();
        try (Connection conn = snowflakeConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.add(new AnalyticsResponse.StatusCount(
                        rs.getString("STATUS_LABEL"),
                        rs.getLong("TOTAL")
                ));
            }
        } catch (SQLException e) {
            logger.error("Snowflake error in getAdminStatusBreakdown: {}", e.getMessage());
        }
        return result;
    }

    @Override
    public List<AnalyticsResponse.TopSender> getTopSenders() {
        // Exclude accounts belonging to users with the ROLE_ADMIN role —
        // admins don't have transfer capability, so they shouldn't appear
        // in a "top spenders" leaderboard.
        String sql = """
                SELECT
                    A.HOLDER_NAME,
                    A.ACCOUNT_ID,
                    COALESCE(SUM(TL.AMOUNT), 0) AS TOTAL_SENT,
                    COUNT(TL.ID)                AS TRANSACTION_COUNT
                FROM ACCOUNTS A
                LEFT JOIN TRANSACTION_LOG TL
                    ON TL.FROM_ACCOUNT_ID = A.ID
                    AND TL.STATUS = 0
                WHERE A.ID NOT IN (
                    SELECT A2.ID
                    FROM ACCOUNTS A2
                    JOIN USERS U ON U.ACCOUNT_ID = A2.ACCOUNT_ID
                    JOIN USER_ROLES UR ON UR.USER_ID = U.ID
                    JOIN ROLES R       ON R.ID = UR.ROLE_ID
                    WHERE R.ROLE_NAME = 'ROLE_ADMIN'
                )
                GROUP BY A.ID, A.HOLDER_NAME, A.ACCOUNT_ID
                ORDER BY TOTAL_SENT DESC
                LIMIT 5
                """;

        List<AnalyticsResponse.TopSender> result = new ArrayList<>();
        try (Connection conn = snowflakeConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.add(new AnalyticsResponse.TopSender(
                        rs.getString("HOLDER_NAME"),
                        rs.getString("ACCOUNT_ID"),
                        rs.getDouble("TOTAL_SENT"),
                        rs.getLong("TRANSACTION_COUNT")
                ));
            }
        } catch (SQLException e) {
            logger.error("Snowflake error in getTopSenders: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch top senders", e);
        }
        return result;
    }

    @Override
    public List<AnalyticsResponse.CategoryCount> getCategoryBreakdown() {
        // Successful transactions only, grouped by category
        String sql = """
                SELECT
                    COALESCE(TD.CATEGORY, 'OTHER') AS CATEGORY,
                    COUNT(*)                        AS TOTAL,
                    COALESCE(SUM(TL.AMOUNT), 0)      AS TOTAL_AMOUNT
                FROM TRANSACTION_LOG TL
                LEFT JOIN TRANSACTION_DETAILS TD ON TD.TRANSACTION_LOG_ID = TL.ID
                WHERE TL.STATUS = 0
                GROUP BY COALESCE(TD.CATEGORY, 'OTHER')
                ORDER BY TOTAL_AMOUNT DESC
                """;

        List<AnalyticsResponse.CategoryCount> result = new ArrayList<>();
        try (Connection conn = snowflakeConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.add(new AnalyticsResponse.CategoryCount(
                        rs.getString("CATEGORY"),
                        rs.getLong("TOTAL"),
                        rs.getDouble("TOTAL_AMOUNT")
                ));
            }
        } catch (SQLException e) {
            logger.error("Snowflake error in getCategoryBreakdown: {}", e.getMessage());
        }
        return result;
    }
}