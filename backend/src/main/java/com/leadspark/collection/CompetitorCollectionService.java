package com.leadspark.collection;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Profile;
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
public class CompetitorCollectionService {
    private final CompetitorCollectionProperties properties;
    private final List<CompetitorDataProvider> dataProviders;
    private final JdbcTemplate jdbcTemplate;

    public CompetitorCollectionService(
            CompetitorCollectionProperties properties,
            List<CompetitorDataProvider> dataProviders,
            JdbcTemplate jdbcTemplate) {
        this.properties = properties;
        this.dataProviders = dataProviders;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Map<String, Object> run(String triggerType) {
        LocalDateTime now = LocalDateTime.now();
        long jobId = System.currentTimeMillis();
        List<String> keywords = properties.getKeywords();

        createJob(jobId, triggerType, keywords.size(), now);

        AtomicInteger collected = new AtomicInteger();
        AtomicInteger inserted = new AtomicInteger();

        try {
            for (String keyword : keywords) {
                for (CompetitorDataProvider dataProvider : dataProviders) {
                    List<CompetitorCompanySignal> signals = dataProvider.collect(keyword);
                    collected.addAndGet(signals.size());
                    for (CompetitorCompanySignal signal : signals) {
                        inserted.addAndGet(insertSignal(jobId, signal, now));
                    }
                }
            }
            finishJob(jobId, "SUCCESS", collected.get(), inserted.get(), null);
            return Map.of(
                    "jobId", jobId,
                    "status", "SUCCESS",
                    "keywordCount", keywords.size(),
                    "collectedCount", collected.get(),
                    "dedupedCount", inserted.get());
        } catch (RuntimeException ex) {
            finishJob(jobId, "FAILED", collected.get(), inserted.get(), ex.getMessage());
            throw ex;
        }
    }

    public List<Map<String, Object>> recentJobs() {
        return jdbcTemplate.queryForList("""
                SELECT id, tenant_id, job_type, status, trigger_type, keyword_count,
                       collected_count, deduped_count, error_message, started_at, finished_at
                FROM competitor_collection_job
                WHERE tenant_id = ?
                ORDER BY started_at DESC
                LIMIT 20
                """, properties.getTenantId());
    }

    public List<Map<String, Object>> recentSignals() {
        return jdbcTemplate.queryForList("""
                SELECT id, job_id, keyword, company_name, product_name, source_type,
                       source_name, source_url, title, summary, confidence, created_at
                FROM competitor_company_signal
                WHERE tenant_id = ?
                ORDER BY created_at DESC
                LIMIT 50
                """, properties.getTenantId());
    }

    private void createJob(long jobId, String triggerType, int keywordCount, LocalDateTime now) {
        jdbcTemplate.update("""
                INSERT INTO competitor_collection_job
                (id, tenant_id, job_type, status, trigger_type, keyword_count,
                 collected_count, deduped_count, started_at, created_at, updated_at)
                VALUES (?, ?, 'COMPETITOR_COMPANY_COLLECTION', 'RUNNING', ?, ?, 0, 0, ?, ?, ?)
                """,
                jobId,
                properties.getTenantId(),
                triggerType,
                keywordCount,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now),
                Timestamp.valueOf(now));
    }

    private int insertSignal(long jobId, CompetitorCompanySignal signal, LocalDateTime now) {
        return jdbcTemplate.update("""
                INSERT IGNORE INTO competitor_company_signal
                (id, tenant_id, job_id, keyword, company_name, normalized_company_name,
                 product_name, source_type, source_name, source_url, title, summary,
                 signal_time, confidence, imported_to_lead, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
                """,
                System.nanoTime(),
                properties.getTenantId(),
                jobId,
                signal.keyword(),
                signal.companyName(),
                normalize(signal.companyName()),
                signal.productName(),
                signal.sourceType(),
                signal.sourceName(),
                signal.sourceUrl(),
                signal.title(),
                signal.summary(),
                signal.signalTime() == null ? null : Timestamp.valueOf(signal.signalTime()),
                signal.confidence(),
                Timestamp.valueOf(now),
                Timestamp.valueOf(now));
    }

    private void finishJob(long jobId, String status, int collected, int inserted, String errorMessage) {
        jdbcTemplate.update("""
                UPDATE competitor_collection_job
                SET status = ?, collected_count = ?, deduped_count = ?, error_message = ?,
                    finished_at = ?, updated_at = ?
                WHERE id = ?
                """,
                status,
                collected,
                inserted,
                errorMessage,
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now()),
                jobId);
    }

    private String normalize(String companyName) {
        return companyName == null ? "" : companyName
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\\\s（）()，,。.;；:：-]", "");
    }
}
