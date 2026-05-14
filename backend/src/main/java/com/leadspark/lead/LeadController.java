package com.leadspark.lead;

import com.leadspark.common.api.ApiResponse;
import com.leadspark.common.api.PageResult;
import com.leadspark.common.error.BizException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/leads")
@Profile("!test")
public class LeadController {
    private static final long TENANT_ID = 1L;

    private final JdbcTemplate jdbcTemplate;

    public LeadController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        long offset = Math.max(page - 1, 0) * pageSize;
        String keywordLike = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        String statusLike = status == null || status.isBlank() ? "%" : status;

        List<Map<String, Object>> items = jdbcTemplate.queryForList("""
                SELECT l.id, l.company_id AS companyId, c.name AS companyName, c.industry,
                       c.region, c.scale, l.source, l.status, l.score, l.grade,
                       l.score_reason AS scoreReason, l.next_follow_up_at AS nextFollowUpAt,
                       l.created_at AS createdAt,
                       COALESCE((
                           SELECT s.content FROM intent_signal s
                           WHERE s.tenant_id = l.tenant_id AND s.company_id = l.company_id
                           ORDER BY s.signal_time DESC, s.created_at DESC LIMIT 1
                       ), '') AS latestSignal
                FROM sales_lead l
                JOIN company c ON c.tenant_id = l.tenant_id AND c.id = l.company_id
                WHERE l.tenant_id = ? AND l.deleted = 0 AND c.deleted = 0
                  AND l.status LIKE ?
                  AND (c.name LIKE ? OR c.industry LIKE ? OR c.region LIKE ?)
                ORDER BY l.score DESC, l.created_at DESC
                LIMIT ? OFFSET ?
                """, TENANT_ID, statusLike, keywordLike, keywordLike, keywordLike, pageSize, offset);

        Long total = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sales_lead l
                JOIN company c ON c.tenant_id = l.tenant_id AND c.id = l.company_id
                WHERE l.tenant_id = ? AND l.deleted = 0 AND c.deleted = 0
                  AND l.status LIKE ?
                  AND (c.name LIKE ? OR c.industry LIKE ? OR c.region LIKE ?)
                """, Long.class, TENANT_ID, statusLike, keywordLike, keywordLike, keywordLike);

        return ApiResponse.success(new PageResult<>(items, page, pageSize, total == null ? 0 : total));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        Map<String, Object> lead = jdbcTemplate.queryForMap("""
                SELECT l.id, l.company_id AS companyId, c.name AS companyName, c.industry,
                       c.region, c.scale, c.website, c.description, l.source, l.status,
                       l.score, l.grade, l.score_reason AS scoreReason,
                       l.last_follow_up_at AS lastFollowUpAt, l.next_follow_up_at AS nextFollowUpAt,
                       l.created_at AS createdAt
                FROM sales_lead l
                JOIN company c ON c.tenant_id = l.tenant_id AND c.id = l.company_id
                WHERE l.tenant_id = ? AND l.id = ? AND l.deleted = 0
                """, TENANT_ID, id);

        Long companyId = ((Number) lead.get("companyId")).longValue();
        List<Map<String, Object>> signals = jdbcTemplate.queryForList("""
                SELECT id, signal_type AS signalType, signal_source AS signalSource,
                       content, signal_time AS signalTime, weight, created_at AS createdAt
                FROM intent_signal
                WHERE tenant_id = ? AND company_id = ?
                ORDER BY signal_time DESC, created_at DESC
                LIMIT 10
                """, TENANT_ID, companyId);
        List<Map<String, Object>> tasks = jdbcTemplate.queryForList("""
                SELECT id, task_type AS taskType, status, title, due_at AS dueAt, result
                FROM sales_task
                WHERE tenant_id = ? AND lead_id = ?
                ORDER BY due_at ASC
                LIMIT 10
                """, TENANT_ID, id);
        List<Map<String, Object>> recommendations = jdbcTemplate.queryForList("""
                SELECT id, recommendation_type AS recommendationType, content, model_name AS modelName,
                       confidence, created_at AS createdAt
                FROM ai_recommendation
                WHERE tenant_id = ? AND target_type = 'LEAD' AND target_id = ?
                ORDER BY created_at DESC
                LIMIT 10
                """, TENANT_ID, id);

        return ApiResponse.success(Map.of(
                "lead", lead,
                "signals", signals,
                "tasks", tasks,
                "recommendations", recommendations));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateLeadRequest request) {
        LocalDateTime now = LocalDateTime.now();
        long companyId = System.nanoTime();
        long leadId = System.nanoTime() + 17;
        long signalId = System.nanoTime() + 31;
        int score = score(request.industry(), request.region(), request.intentSignal());

        jdbcTemplate.update("""
                INSERT INTO company
                (id, tenant_id, name, normalized_name, industry, region, scale, website,
                 description, data_quality_score, deleted, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
                """,
                companyId,
                TENANT_ID,
                request.companyName(),
                normalize(request.companyName()),
                defaultValue(request.industry(), "待识别"),
                defaultValue(request.region(), "未知"),
                defaultValue(request.scale(), "未知"),
                request.website(),
                request.description(),
                70,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now));

        jdbcTemplate.update("""
                INSERT INTO sales_lead
                (id, tenant_id, company_id, source, source_ref, status, score, grade,
                 score_reason, next_follow_up_at, deleted, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'NEW', ?, ?, ?, ?, 0, ?, ?)
                """,
                leadId,
                TENANT_ID,
                companyId,
                defaultValue(request.source(), "MANUAL"),
                request.sourceRef(),
                score,
                grade(score),
                reason(score, request.intentSignal()),
                Timestamp.valueOf(now.plusDays(1)),
                Timestamp.valueOf(now),
                Timestamp.valueOf(now));

        if (request.intentSignal() != null && !request.intentSignal().isBlank()) {
            jdbcTemplate.update("""
                    INSERT INTO intent_signal
                    (id, tenant_id, company_id, signal_type, signal_source, content,
                     signal_time, weight, created_at)
                    VALUES (?, ?, ?, 'MANUAL', ?, ?, ?, ?, ?)
                    """,
                    signalId,
                    TENANT_ID,
                    companyId,
                    defaultValue(request.source(), "MANUAL"),
                    request.intentSignal(),
                    Timestamp.valueOf(now),
                    Math.min(score, 90),
                    Timestamp.valueOf(now));
        }

        return ApiResponse.success(Map.of(
                "id", leadId,
                "companyId", companyId,
                "companyName", request.companyName(),
                "status", "NEW",
                "score", score,
                "grade", grade(score)));
    }

    private int score(String industry, String region, String signal) {
        int value = 50;
        if (containsAny(industry, "软件", "SaaS", "企业服务", "智能制造", "数字化")) {
            value += 18;
        }
        if (containsAny(region, "上海", "杭州", "苏州", "深圳", "北京", "广州")) {
            value += 10;
        }
        if (containsAny(signal, "招聘", "融资", "扩张", "数字化", "销售", "获客", "招标")) {
            value += 18;
        }
        return Math.min(value, 99);
    }

    private boolean containsAny(String text, String... words) {
        if (text == null) {
            return false;
        }
        for (String word : words) {
            if (text.toLowerCase(Locale.ROOT).contains(word.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String grade(int score) {
        if (score >= 90) {
            return "S";
        }
        if (score >= 80) {
            return "A";
        }
        if (score >= 65) {
            return "B";
        }
        return "C";
    }

    private String reason(int score, String signal) {
        if (signal == null || signal.isBlank()) {
            return "基于行业、地区和企业基础画像计算初始分。";
        }
        return "识别到意图信号：" + signal + "；综合画像评分为 " + score + "。";
    }

    private String normalize(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            throw new BizException(400, "companyName is required");
        }
        return companyName.toLowerCase(Locale.ROOT).replaceAll("[\\\\s（）()，,。.;；:：-]", "");
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public record CreateLeadRequest(
            @NotBlank String companyName,
            String source,
            String sourceRef,
            String industry,
            String region,
            String scale,
            String website,
            String description,
            String intentSignal) {
    }
}
