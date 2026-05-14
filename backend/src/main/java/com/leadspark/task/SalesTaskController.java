package com.leadspark.task;

import com.leadspark.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
@Profile("!test")
public class SalesTaskController {
    private static final long TENANT_ID = 1L;
    private static final long DEFAULT_OWNER = 1L;

    private final JdbcTemplate jdbcTemplate;

    public SalesTaskController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "30") int limit) {
        String statusLike = status == null || status.isBlank() ? "%" : status;
        return ApiResponse.success(jdbcTemplate.queryForList("""
                SELECT t.id, t.lead_id AS leadId, t.company_id AS companyId, c.name AS companyName,
                       t.task_type AS taskType, t.status, t.title, t.due_at AS dueAt,
                       t.completed_at AS completedAt, t.result
                FROM sales_task t
                JOIN company c ON c.tenant_id = t.tenant_id AND c.id = t.company_id
                WHERE t.tenant_id = ? AND t.status LIKE ?
                ORDER BY FIELD(t.status, 'PENDING', 'DONE', 'CANCELLED'), t.due_at ASC
                LIMIT ?
                """, TENANT_ID, statusLike, limit));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateTaskRequest request) {
        Map<String, Object> lead = jdbcTemplate.queryForMap("""
                SELECT id, company_id AS companyId FROM sales_lead
                WHERE tenant_id = ? AND id = ? AND deleted = 0
                """, TENANT_ID, request.leadId());
        long id = System.nanoTime();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueAt = request.dueAt() == null ? now.plusDays(1) : request.dueAt();

        jdbcTemplate.update("""
                INSERT INTO sales_task
                (id, tenant_id, lead_id, company_id, owner_user_id, task_type, status,
                 title, due_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 'PENDING', ?, ?, ?, ?)
                """,
                id,
                TENANT_ID,
                request.leadId(),
                ((Number) lead.get("companyId")).longValue(),
                request.ownerUserId() == null ? DEFAULT_OWNER : request.ownerUserId(),
                request.taskType(),
                request.title(),
                Timestamp.valueOf(dueAt),
                Timestamp.valueOf(now),
                Timestamp.valueOf(now));

        return ApiResponse.success(Map.of("id", id, "leadId", request.leadId(), "status", "PENDING"));
    }

    @PatchMapping("/{id}/complete")
    public ApiResponse<Map<String, Object>> complete(
            @PathVariable Long id,
            @RequestBody(required = false) CompleteTaskRequest request) {
        String result = request == null || request.result() == null ? "DONE" : request.result();
        jdbcTemplate.update("""
                UPDATE sales_task
                SET status = 'DONE', result = ?, completed_at = ?, updated_at = ?
                WHERE tenant_id = ? AND id = ?
                """,
                result,
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now()),
                TENANT_ID,
                id);
        return ApiResponse.success(Map.of("id", id, "status", "DONE", "result", result));
    }

    public record CreateTaskRequest(
            @NotNull Long leadId,
            @NotBlank String taskType,
            @NotBlank String title,
            LocalDateTime dueAt,
            Long ownerUserId) {
    }

    public record CompleteTaskRequest(String result) {
    }
}
