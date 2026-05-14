package com.leadspark.analytics;

import com.leadspark.common.api.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@Profile("!test")
public class AnalyticsController {
    private static final long TENANT_ID = 1L;

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/workbench")
    public ApiResponse<Map<String, Object>> workbench() {
        Integer todayTasks = number("""
                SELECT COUNT(*) FROM sales_task
                WHERE tenant_id = ? AND status = 'PENDING' AND DATE(due_at) = CURRENT_DATE
                """);
        Integer overdueTasks = number("""
                SELECT COUNT(*) FROM sales_task
                WHERE tenant_id = ? AND status = 'PENDING' AND due_at < NOW()
                """);
        Integer highScoreLeads = number("""
                SELECT COUNT(*) FROM sales_lead
                WHERE tenant_id = ? AND deleted = 0 AND score >= 80
                """);
        Integer weeklyTouches = number("""
                SELECT COUNT(*) FROM follow_up_record
                WHERE tenant_id = ? AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
                """);
        BigDecimal monthlyDealAmount = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(amount), 0) FROM opportunity
                WHERE tenant_id = ? AND status = 'OPEN'
                """, BigDecimal.class, TENANT_ID);

        List<Map<String, Object>> scoreBuckets = jdbcTemplate.queryForList("""
                SELECT grade, COUNT(*) AS count
                FROM sales_lead
                WHERE tenant_id = ? AND deleted = 0
                GROUP BY grade
                ORDER BY FIELD(grade, 'S', 'A', 'B', 'C')
                """, TENANT_ID);
        List<Map<String, Object>> sourceStats = jdbcTemplate.queryForList("""
                SELECT source, COUNT(*) AS count, ROUND(AVG(score), 1) AS avgScore
                FROM sales_lead
                WHERE tenant_id = ? AND deleted = 0
                GROUP BY source
                ORDER BY count DESC
                """, TENANT_ID);

        return ApiResponse.success(Map.of(
                "todayTasks", todayTasks,
                "overdueTasks", overdueTasks,
                "newHighScoreLeads", highScoreLeads,
                "weeklyTouches", weeklyTouches,
                "monthlyDealAmount", monthlyDealAmount == null ? BigDecimal.ZERO : monthlyDealAmount,
                "scoreBuckets", scoreBuckets,
                "sourceStats", sourceStats));
    }

    private Integer number(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, TENANT_ID);
        return value == null ? 0 : value;
    }
}
