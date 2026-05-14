package com.leadspark.importexport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Profile("!test")
public class LeadImportService {
    private static final long TENANT_ID = 1L;
    private static final long DEFAULT_OWNER = 1L;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public LeadImportService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> importRows(List<LeadImportRow> rows, String sourceType, String fileName) {
        long taskId = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();
        createTask(taskId, "LEAD_IMPORT", sourceType, fileName, rows.size(), toJson(Map.of("rows", rows.size())), now);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        AtomicInteger duplicate = new AtomicInteger();

        for (int i = 0; i < rows.size(); i++) {
            LeadImportRow row = rows.get(i);
            try {
                ImportOutcome outcome = importOne(row, sourceType, row.sourceRef(), now);
                success.incrementAndGet();
                if (outcome.duplicate()) {
                    duplicate.incrementAndGet();
                }
            } catch (RuntimeException ex) {
                failed.incrementAndGet();
                recordError(taskId, i + 1, row.sourceRef(), "ROW_IMPORT_FAILED", ex.getMessage(), toJson(row), now);
            }
        }

        finishTask(taskId, status(success.get(), failed.get()), success.get(), failed.get(), duplicate.get(), null);
        return summary(taskId, rows.size(), success.get(), failed.get(), duplicate.get());
    }

    @Transactional
    public Map<String, Object> importCompetitorSignals(CompetitorSignalImportRequest request) {
        int limit = request.limit() == null ? 100 : Math.max(1, Math.min(request.limit(), 500));
        int minConfidence = request.minConfidence() == null ? 0 : request.minConfidence();
        List<Map<String, Object>> signals = jdbcTemplate.queryForList("""
                SELECT id, keyword, company_name, product_name, source_type, source_name,
                       source_url, title, summary, confidence, signal_time
                FROM competitor_company_signal
                WHERE tenant_id = ? AND imported_to_lead = 0 AND confidence >= ?
                ORDER BY confidence DESC, created_at DESC
                LIMIT ?
                """, TENANT_ID, minConfidence, limit);

        long taskId = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();
        createTask(taskId, "LEAD_IMPORT", "COMPETITOR_SIGNAL", null, signals.size(), toJson(request), now);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        AtomicInteger duplicate = new AtomicInteger();

        for (int i = 0; i < signals.size(); i++) {
            Map<String, Object> signal = signals.get(i);
            Long signalId = ((Number) signal.get("id")).longValue();
            try {
                LeadImportRow row = rowFromSignal(signal);
                ImportOutcome outcome = importOne(row, "COMPETITOR_SIGNAL", String.valueOf(signalId), now);
                jdbcTemplate.update("""
                        UPDATE competitor_company_signal
                        SET imported_to_lead = 1, updated_at = ?
                        WHERE tenant_id = ? AND id = ?
                        """, Timestamp.valueOf(LocalDateTime.now()), TENANT_ID, signalId);
                success.incrementAndGet();
                if (outcome.duplicate()) {
                    duplicate.incrementAndGet();
                }
            } catch (RuntimeException ex) {
                failed.incrementAndGet();
                recordError(taskId, i + 1, String.valueOf(signalId), "SIGNAL_IMPORT_FAILED", ex.getMessage(), toJson(signal), now);
            }
        }

        finishTask(taskId, status(success.get(), failed.get()), success.get(), failed.get(), duplicate.get(), null);
        return summary(taskId, signals.size(), success.get(), failed.get(), duplicate.get());
    }

    public List<Map<String, Object>> recentTasks() {
        return jdbcTemplate.queryForList("""
                SELECT id AS taskId, task_type AS taskType, source_type AS sourceType, status,
                       file_name AS fileName, total_rows AS totalRows, success_rows AS successRows,
                       failed_rows AS failedRows, duplicate_rows AS duplicateRows,
                       error_message AS errorMessage, started_at AS startedAt,
                       finished_at AS finishedAt, created_at AS createdAt
                FROM import_task
                WHERE tenant_id = ?
                ORDER BY created_at DESC
                LIMIT 30
                """, TENANT_ID);
    }

    public Map<String, Object> task(Long taskId) {
        return jdbcTemplate.queryForMap("""
                SELECT id AS taskId, task_type AS taskType, source_type AS sourceType, status,
                       file_name AS fileName, total_rows AS totalRows, success_rows AS successRows,
                       failed_rows AS failedRows, duplicate_rows AS duplicateRows,
                       request_payload AS requestPayload, error_message AS errorMessage,
                       started_at AS startedAt, finished_at AS finishedAt, created_at AS createdAt
                FROM import_task
                WHERE tenant_id = ? AND id = ?
                """, TENANT_ID, taskId);
    }

    public List<Map<String, Object>> taskErrors(Long taskId) {
        return jdbcTemplate.queryForList("""
                SELECT id, task_id AS taskId, row_index AS rowNumber, source_ref AS sourceRef,
                       error_code AS errorCode, error_message AS errorMessage,
                       raw_data AS rawData, created_at AS createdAt
                FROM import_task_error
                WHERE tenant_id = ? AND task_id = ?
                ORDER BY row_index ASC, created_at ASC
                """, TENANT_ID, taskId);
    }

    private ImportOutcome importOne(LeadImportRow row, String sourceType, String sourceRef, LocalDateTime now) {
        if (row.companyName() == null || row.companyName().isBlank()) {
            throw new IllegalArgumentException("companyName is required");
        }

        String normalizedName = normalize(row.companyName());
        long companyId = findCompanyId(normalizedName);
        boolean companyExists = companyId > 0;
        if (!companyExists) {
            companyId = System.nanoTime();
            jdbcTemplate.update("""
                    INSERT INTO company
                    (id, tenant_id, name, normalized_name, industry, region, scale, website,
                     description, data_quality_score, deleted, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
                    """,
                    companyId,
                    TENANT_ID,
                    row.companyName(),
                    normalizedName,
                    defaultValue(row.industry(), "待识别"),
                    defaultValue(row.region(), "未知"),
                    defaultValue(row.scale(), "未知"),
                    row.website(),
                    row.description(),
                    quality(row),
                    Timestamp.valueOf(now),
                    Timestamp.valueOf(now));
        } else {
            jdbcTemplate.update("""
                    UPDATE company
                    SET industry = COALESCE(NULLIF(?, ''), industry),
                        region = COALESCE(NULLIF(?, ''), region),
                        scale = COALESCE(NULLIF(?, ''), scale),
                        website = COALESCE(NULLIF(?, ''), website),
                        description = COALESCE(NULLIF(?, ''), description),
                        updated_at = ?
                    WHERE tenant_id = ? AND id = ?
                    """,
                    empty(row.industry()),
                    empty(row.region()),
                    empty(row.scale()),
                    empty(row.website()),
                    empty(row.description()),
                    Timestamp.valueOf(now),
                    TENANT_ID,
                    companyId);
        }

        long leadId = findLeadId(companyId);
        boolean duplicate = leadId > 0;
        int score = score(row);
        if (!duplicate) {
            leadId = System.nanoTime() + 17;
            jdbcTemplate.update("""
                    INSERT INTO sales_lead
                    (id, tenant_id, company_id, source, source_ref, status, owner_user_id,
                     score, grade, score_reason, next_follow_up_at, deleted, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, 'NEW', ?, ?, ?, ?, ?, 0, ?, ?)
                    """,
                    leadId,
                    TENANT_ID,
                    companyId,
                    sourceType,
                    sourceRef,
                    DEFAULT_OWNER,
                    score,
                    grade(score),
                    reason(row, score),
                    Timestamp.valueOf(now.plusDays(1)),
                    Timestamp.valueOf(now),
                    Timestamp.valueOf(now));
        } else {
            jdbcTemplate.update("""
                    UPDATE sales_lead
                    SET score = GREATEST(score, ?), grade = ?,
                        score_reason = COALESCE(NULLIF(?, ''), score_reason),
                        updated_at = ?
                    WHERE tenant_id = ? AND id = ?
                    """,
                    score,
                    grade(score),
                    reason(row, score),
                    Timestamp.valueOf(now),
                    TENANT_ID,
                    leadId);
        }

        if (row.intentSignal() != null && !row.intentSignal().isBlank()) {
            jdbcTemplate.update("""
                    INSERT INTO intent_signal
                    (id, tenant_id, company_id, signal_type, signal_source, content,
                     signal_time, weight, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    System.nanoTime() + 31,
                    TENANT_ID,
                    companyId,
                    row.signalType() == null ? "IMPORT" : row.signalType(),
                    sourceType,
                    row.intentSignal(),
                    row.signalTime() == null ? Timestamp.valueOf(now) : Timestamp.valueOf(row.signalTime()),
                    Math.min(score, 95),
                    Timestamp.valueOf(now));
        }

        if (!duplicate) {
            jdbcTemplate.update("""
                    INSERT INTO sales_task
                    (id, tenant_id, lead_id, company_id, owner_user_id, task_type, status,
                     title, due_at, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, 'CALL', 'PENDING', ?, ?, ?, ?)
                    """,
                    System.nanoTime() + 47,
                    TENANT_ID,
                    leadId,
                    companyId,
                    DEFAULT_OWNER,
                    "首次触达：" + row.companyName(),
                    Timestamp.valueOf(now.plusDays(1)),
                    Timestamp.valueOf(now),
                    Timestamp.valueOf(now));
        }

        return new ImportOutcome(leadId, companyId, duplicate);
    }

    private LeadImportRow rowFromSignal(Map<String, Object> signal) {
        String title = string(signal.get("title"));
        String summary = string(signal.get("summary"));
        String keyword = string(signal.get("keyword"));
        String intent = (title + " " + summary).trim();
        if (intent.isBlank()) {
            intent = "命中同类产品关键词：" + keyword;
        }
        return new LeadImportRow(
                string(signal.get("company_name")),
                "待识别",
                "未知",
                "未知",
                string(signal.get("source_url")),
                summary,
                intent,
                "COMPETITOR_" + string(signal.get("source_type")),
                toLocalDateTime(signal.get("signal_time")),
                keyword,
                string(signal.get("id")));
    }

    private void createTask(long taskId, String taskType, String sourceType, String fileName, int totalRows, String requestPayload, LocalDateTime now) {
        jdbcTemplate.update("""
                INSERT INTO import_task
                (id, tenant_id, task_type, source_type, status, file_name, total_rows,
                 request_payload, started_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'PROCESSING', ?, ?, CAST(? AS JSON), ?, ?, ?)
                """,
                taskId,
                TENANT_ID,
                taskType,
                sourceType,
                fileName,
                totalRows,
                requestPayload,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now),
                Timestamp.valueOf(now));
    }

    private void finishTask(long taskId, String status, int success, int failed, int duplicate, String errorMessage) {
        jdbcTemplate.update("""
                UPDATE import_task
                SET status = ?, success_rows = ?, failed_rows = ?, duplicate_rows = ?,
                    error_message = ?, finished_at = ?, updated_at = ?
                WHERE tenant_id = ? AND id = ?
                """,
                status,
                success,
                failed,
                duplicate,
                errorMessage,
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now()),
                TENANT_ID,
                taskId);
    }

    private void recordError(long taskId, int rowNumber, String sourceRef, String code, String message, String rawData, LocalDateTime now) {
        jdbcTemplate.update("""
                INSERT INTO import_task_error
                (id, tenant_id, task_id, row_index, source_ref, error_code,
                 error_message, raw_data, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), ?)
                """,
                System.nanoTime(),
                TENANT_ID,
                taskId,
                rowNumber,
                sourceRef,
                code,
                message == null ? "unknown error" : message,
                rawData,
                Timestamp.valueOf(now));
    }

    private long findCompanyId(String normalizedName) {
        List<Long> ids = jdbcTemplate.queryForList("""
                SELECT id FROM company
                WHERE tenant_id = ? AND normalized_name = ? AND deleted = 0
                ORDER BY created_at ASC
                LIMIT 1
                """, Long.class, TENANT_ID, normalizedName);
        return ids.isEmpty() ? 0 : ids.get(0);
    }

    private long findLeadId(long companyId) {
        List<Long> ids = jdbcTemplate.queryForList("""
                SELECT id FROM sales_lead
                WHERE tenant_id = ? AND company_id = ? AND deleted = 0
                ORDER BY created_at ASC
                LIMIT 1
                """, Long.class, TENANT_ID, companyId);
        return ids.isEmpty() ? 0 : ids.get(0);
    }

    private Map<String, Object> summary(long taskId, int total, int success, int failed, int duplicate) {
        return Map.of(
                "taskId", taskId,
                "status", status(success, failed),
                "totalRows", total,
                "successRows", success,
                "failedRows", failed,
                "duplicateRows", duplicate);
    }

    private String status(int success, int failed) {
        if (failed == 0) {
            return "SUCCESS";
        }
        return success > 0 ? "PARTIAL_SUCCESS" : "FAILED";
    }

    private int score(LeadImportRow row) {
        int value = 48;
        if (containsAny(row.industry(), "软件", "SaaS", "企业服务", "智能制造", "数字化")) {
            value += 18;
        }
        if (containsAny(row.region(), "上海", "杭州", "苏州", "深圳", "北京", "广州")) {
            value += 10;
        }
        if (containsAny(row.intentSignal(), "招聘", "融资", "扩张", "数字化", "销售", "获客", "招标", "同类产品")) {
            value += 20;
        }
        return Math.min(value, 99);
    }

    private int quality(LeadImportRow row) {
        int value = 50;
        if (row.industry() != null && !row.industry().isBlank()) {
            value += 15;
        }
        if (row.region() != null && !row.region().isBlank()) {
            value += 15;
        }
        if (row.website() != null && !row.website().isBlank()) {
            value += 10;
        }
        return Math.min(value, 95);
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

    private String reason(LeadImportRow row, int score) {
        if (row.intentSignal() == null || row.intentSignal().isBlank()) {
            return "导入后按企业画像生成初始评分：" + score + "。";
        }
        return "导入信号：" + row.intentSignal() + "；综合评分：" + score + "。";
    }

    private String normalize(String companyName) {
        return companyName.toLowerCase(Locale.ROOT).replaceAll("[\\\\s（）()，,。.;；:：-]", "");
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String empty(String value) {
        return value == null ? "" : value;
    }

    private String string(Object value) {
        return value == null ? "" : value.toString();
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        return null;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    public record LeadImportRow(
            String companyName,
            String industry,
            String region,
            String scale,
            String website,
            String description,
            String intentSignal,
            String signalType,
            LocalDateTime signalTime,
            String source,
            String sourceRef) {
    }

    public record LeadImportRequest(
            String sourceType,
            String fileName,
            List<LeadImportRow> rows) {
    }

    public record CompetitorSignalImportRequest(Integer limit, Integer minConfidence) {
    }

    private record ImportOutcome(long leadId, long companyId, boolean duplicate) {
    }
}
