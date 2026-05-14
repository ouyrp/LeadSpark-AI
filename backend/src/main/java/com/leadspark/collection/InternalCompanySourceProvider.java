package com.leadspark.collection;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;

@Component
@Profile("!test")
public class InternalCompanySourceProvider implements CompetitorDataProvider {
    private final CompetitorCollectionProperties properties;
    private final JdbcTemplate jdbcTemplate;

    public InternalCompanySourceProvider(
            CompetitorCollectionProperties properties,
            JdbcTemplate jdbcTemplate) {
        this.properties = properties;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String sourceName() {
        return "internal";
    }

    @Override
    public List<CompetitorCompanySignal> collect(String keyword) {
        if (!properties.getSources().getInternal().isEnabled()) {
            return List.of();
        }

        return jdbcTemplate.query("""
                        SELECT company_name, product_name, source_name, source_url,
                               signal_title, signal_summary, confidence, updated_at
                        FROM internal_company_source
                        WHERE tenant_id = ?
                          AND status = 'ACTIVE'
                          AND (
                              keywords LIKE CONCAT('%', ?, '%')
                              OR company_name LIKE CONCAT('%', ?, '%')
                              OR product_name LIKE CONCAT('%', ?, '%')
                              OR signal_summary LIKE CONCAT('%', ?, '%')
                          )
                        ORDER BY updated_at DESC
                        LIMIT 100
                        """,
                (rs, rowNum) -> new CompetitorCompanySignal(
                        keyword,
                        rs.getString("company_name"),
                        rs.getString("product_name"),
                        "INTERNAL_DB",
                        rs.getString("source_name"),
                        rs.getString("source_url"),
                        rs.getString("signal_title"),
                        rs.getString("signal_summary"),
                        toLocalDateTime(rs.getTimestamp("updated_at")),
                        rs.getInt("confidence")),
                properties.getTenantId(),
                keyword,
                keyword,
                keyword,
                keyword);
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
