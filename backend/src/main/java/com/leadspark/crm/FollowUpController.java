package com.leadspark.crm;

import com.leadspark.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/leads/{leadId}/follow-ups")
@Profile("!test")
public class FollowUpController {
    private static final long TENANT_ID = 1L;
    private static final long DEFAULT_USER = 1L;

    private final JdbcTemplate jdbcTemplate;

    public FollowUpController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@PathVariable Long leadId) {
        return ApiResponse.success(jdbcTemplate.queryForList("""
                SELECT id, lead_id AS leadId, company_id AS companyId, user_id AS userId,
                       channel, result, content, next_action AS nextAction,
                       next_follow_up_at AS nextFollowUpAt, created_at AS createdAt
                FROM follow_up_record
                WHERE tenant_id = ? AND lead_id = ?
                ORDER BY created_at DESC
                LIMIT 30
                """, TENANT_ID, leadId));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(
            @PathVariable Long leadId,
            @Valid @RequestBody CreateFollowUpRequest request) {
        Map<String, Object> lead = jdbcTemplate.queryForMap("""
                SELECT id, company_id AS companyId FROM sales_lead
                WHERE tenant_id = ? AND id = ? AND deleted = 0
                """, TENANT_ID, leadId);
        long id = System.nanoTime();
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update("""
                INSERT INTO follow_up_record
                (id, tenant_id, lead_id, company_id, user_id, channel, content, result,
                 next_action, next_follow_up_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                TENANT_ID,
                leadId,
                ((Number) lead.get("companyId")).longValue(),
                request.userId() == null ? DEFAULT_USER : request.userId(),
                request.channel(),
                request.content(),
                request.result(),
                request.nextAction(),
                request.nextFollowUpAt() == null ? null : Timestamp.valueOf(request.nextFollowUpAt()),
                Timestamp.valueOf(now));

        jdbcTemplate.update("""
                UPDATE sales_lead
                SET status = ?, last_follow_up_at = ?, next_follow_up_at = ?, updated_at = ?
                WHERE tenant_id = ? AND id = ?
                """,
                request.result().equals("INTERESTED") ? "FOLLOWING" : "TOUCHING",
                Timestamp.valueOf(now),
                request.nextFollowUpAt() == null ? null : Timestamp.valueOf(request.nextFollowUpAt()),
                Timestamp.valueOf(now),
                TENANT_ID,
                leadId);

        return ApiResponse.success(Map.of("id", id, "leadId", leadId));
    }

    public record CreateFollowUpRequest(
            @NotBlank String channel,
            @NotBlank String content,
            @NotBlank String result,
            String nextAction,
            LocalDateTime nextFollowUpAt,
            Long userId) {
    }
}
