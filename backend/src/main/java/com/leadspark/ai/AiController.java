package com.leadspark.ai;

import com.leadspark.common.api.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@Profile("!test")
public class AiController {
    private static final long TENANT_ID = 1L;

    private final JdbcTemplate jdbcTemplate;

    public AiController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/leads/{leadId}/score")
    public ApiResponse<Map<String, Object>> scoreLead(@PathVariable Long leadId) {
        Map<String, Object> lead = lead(leadId);
        Integer signalWeight = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(weight), 0) FROM intent_signal
                WHERE tenant_id = ? AND company_id = ?
                """, Integer.class, TENANT_ID, lead.get("companyId"));
        int base = scoreText(lead.get("industry")) + scoreText(lead.get("region"));
        int score = Math.min(99, 45 + base + Math.min(signalWeight == null ? 0 : signalWeight / 4, 30));
        String grade = grade(score);
        String reason = "AI 评分综合企业行业、区域、规模和近期意图信号，当前建议优先级为 " + grade + "。";

        jdbcTemplate.update("""
                UPDATE sales_lead
                SET score = ?, grade = ?, score_reason = ?, updated_at = ?
                WHERE tenant_id = ? AND id = ?
                """, score, grade, reason, Timestamp.valueOf(LocalDateTime.now()), TENANT_ID, leadId);
        saveRecommendation(leadId, "LEAD_SCORE", reason, score);

        return ApiResponse.success(Map.of(
                "leadId", leadId,
                "score", score,
                "grade", grade,
                "reason", reason));
    }

    @PostMapping("/leads/{leadId}/pitch")
    public ApiResponse<Map<String, Object>> generatePitch(@PathVariable Long leadId) {
        Map<String, Object> lead = lead(leadId);
        String content = "你好，我关注到" + lead.get("companyName")
                + "在" + lead.get("industry") + "方向有增长信号。我们可以帮销售团队把公开企业信号、客户画像和跟进动作串起来，优先触达更可能转化的客户。";
        saveRecommendation(leadId, "PITCH", content, 82);
        return ApiResponse.success(Map.of(
                "leadId", leadId,
                "channel", "CALL",
                "content", content));
    }

    private Map<String, Object> lead(Long leadId) {
        return jdbcTemplate.queryForMap("""
                SELECT l.id, l.company_id AS companyId, c.name AS companyName,
                       c.industry, c.region, c.scale, l.score
                FROM sales_lead l
                JOIN company c ON c.tenant_id = l.tenant_id AND c.id = l.company_id
                WHERE l.tenant_id = ? AND l.id = ? AND l.deleted = 0
                """, TENANT_ID, leadId);
    }

    private void saveRecommendation(Long leadId, String type, String content, int confidence) {
        jdbcTemplate.update("""
                INSERT INTO ai_recommendation
                (id, tenant_id, target_type, target_id, recommendation_type, content,
                 model_name, prompt_version, confidence, created_at)
                VALUES (?, ?, 'LEAD', ?, ?, ?, 'mock-ai', 'v1', ?, ?)
                """,
                System.nanoTime(),
                TENANT_ID,
                leadId,
                type,
                content,
                confidence,
                Timestamp.valueOf(LocalDateTime.now()));
    }

    private int scoreText(Object value) {
        String text = value == null ? "" : value.toString();
        int score = 0;
        if (text.contains("软件") || text.contains("企业服务") || text.contains("智能制造")) {
            score += 14;
        }
        if (text.contains("上海") || text.contains("杭州") || text.contains("苏州") || text.contains("深圳")) {
            score += 10;
        }
        return score;
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
}
