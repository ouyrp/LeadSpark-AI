package com.leadspark.crm;

import com.leadspark.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/opportunities")
@Profile("!test")
public class OpportunityController {
    private static final long TENANT_ID = 1L;
    private static final long DEFAULT_OWNER = 1L;

    private final JdbcTemplate jdbcTemplate;

    public OpportunityController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.success(jdbcTemplate.queryForList("""
                SELECT o.id, o.lead_id AS leadId, o.company_id AS companyId, c.name AS companyName,
                       o.stage, o.amount, o.probability, o.expected_close_date AS expectedCloseDate,
                       o.status, o.lost_reason AS lostReason, o.created_at AS createdAt
                FROM opportunity o
                JOIN company c ON c.tenant_id = o.tenant_id AND c.id = o.company_id
                WHERE o.tenant_id = ?
                ORDER BY o.created_at DESC
                LIMIT 50
                """, TENANT_ID));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateOpportunityRequest request) {
        Map<String, Object> lead = jdbcTemplate.queryForMap("""
                SELECT id, company_id AS companyId FROM sales_lead
                WHERE tenant_id = ? AND id = ? AND deleted = 0
                """, TENANT_ID, request.leadId());
        long id = System.nanoTime();
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update("""
                INSERT INTO opportunity
                (id, tenant_id, lead_id, company_id, owner_user_id, stage, amount,
                 probability, expected_close_date, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'OPEN', ?, ?)
                """,
                id,
                TENANT_ID,
                request.leadId(),
                ((Number) lead.get("companyId")).longValue(),
                request.ownerUserId() == null ? DEFAULT_OWNER : request.ownerUserId(),
                request.stage() == null ? "QUALIFIED" : request.stage(),
                request.amount(),
                request.probability() == null ? 50 : request.probability(),
                request.expectedCloseDate() == null ? null : Date.valueOf(request.expectedCloseDate()),
                Timestamp.valueOf(now),
                Timestamp.valueOf(now));

        jdbcTemplate.update("""
                UPDATE sales_lead SET status = 'QUALIFIED', updated_at = ?
                WHERE tenant_id = ? AND id = ?
                """, Timestamp.valueOf(now), TENANT_ID, request.leadId());

        return ApiResponse.success(Map.of("id", id, "stage", request.stage() == null ? "QUALIFIED" : request.stage(), "status", "OPEN"));
    }

    @PatchMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOpportunityRequest request) {
        String stage = request.stage() == null ? "QUALIFIED" : request.stage();
        String status = request.status() == null ? "OPEN" : request.status();
        jdbcTemplate.update("""
                UPDATE opportunity
                SET stage = ?, probability = ?, status = ?, lost_reason = ?, updated_at = ?
                WHERE tenant_id = ? AND id = ?
                """,
                stage,
                request.probability() == null ? 50 : request.probability(),
                status,
                request.lostReason(),
                Timestamp.valueOf(LocalDateTime.now()),
                TENANT_ID,
                id);

        if ("WON".equals(status) || "LOST".equals(status)) {
            jdbcTemplate.update("""
                    UPDATE sales_lead l
                    JOIN opportunity o ON o.tenant_id = l.tenant_id AND o.lead_id = l.id
                    SET l.status = ?, l.updated_at = ?
                    WHERE o.tenant_id = ? AND o.id = ?
                    """,
                    status,
                    Timestamp.valueOf(LocalDateTime.now()),
                    TENANT_ID,
                    id);
        }

        return ApiResponse.success(Map.of("id", id, "stage", stage, "status", status));
    }

    public record CreateOpportunityRequest(
            @NotNull Long leadId,
            String stage,
            BigDecimal amount,
            Integer probability,
            LocalDate expectedCloseDate,
            Long ownerUserId) {
    }

    public record UpdateOpportunityRequest(
            String stage,
            Integer probability,
            String status,
            String lostReason) {
    }
}
