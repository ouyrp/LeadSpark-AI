package com.leadspark.analytics;

import com.leadspark.common.api.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
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
        List<Map<String, Object>> statusStats = jdbcTemplate.queryForList("""
                SELECT status, COUNT(*) AS count
                FROM sales_lead
                WHERE tenant_id = ? AND deleted = 0
                GROUP BY status
                ORDER BY count DESC
                """, TENANT_ID);
        List<Map<String, Object>> taskStats = jdbcTemplate.queryForList("""
                SELECT status, COUNT(*) AS count
                FROM sales_task
                WHERE tenant_id = ?
                GROUP BY status
                ORDER BY count DESC
                """, TENANT_ID);
        List<Map<String, Object>> opportunityFunnel = jdbcTemplate.queryForList("""
                SELECT stage, status, COUNT(*) AS count, COALESCE(SUM(amount), 0) AS amount,
                       ROUND(AVG(probability), 1) AS avgProbability
                FROM opportunity
                WHERE tenant_id = ?
                GROUP BY stage, status
                ORDER BY FIELD(stage, 'QUALIFIED', 'DEMO', 'PROPOSAL', 'NEGOTIATION', 'CLOSED'), status
                """, TENANT_ID);
        List<Map<String, Object>> leadTrend = jdbcTemplate.queryForList("""
                SELECT DATE(created_at) AS date, COUNT(*) AS count
                FROM sales_lead
                WHERE tenant_id = ? AND deleted = 0
                  AND created_at >= DATE_SUB(CURRENT_DATE, INTERVAL 13 DAY)
                GROUP BY DATE(created_at)
                ORDER BY date ASC
                """, TENANT_ID);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todayTasks", todayTasks);
        result.put("overdueTasks", overdueTasks);
        result.put("newHighScoreLeads", highScoreLeads);
        result.put("weeklyTouches", weeklyTouches);
        result.put("monthlyDealAmount", monthlyDealAmount == null ? BigDecimal.ZERO : monthlyDealAmount);
        result.put("scoreBuckets", scoreBuckets);
        result.put("sourceStats", sourceStats);
        result.put("statusStats", statusStats);
        result.put("taskStats", taskStats);
        result.put("opportunityFunnel", opportunityFunnel);
        result.put("leadTrend", leadTrend);
        return ApiResponse.success(result);
    }

    private Integer number(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, TENANT_ID);
        return value == null ? 0 : value;
    }
}
