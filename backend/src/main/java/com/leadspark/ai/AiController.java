package com.leadspark.ai;

import com.leadspark.common.api.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
    @PostMapping("/leads/{leadId}/score")
    public ApiResponse<Map<String, Object>> scoreLead(@PathVariable Long leadId) {
        return ApiResponse.success(Map.of(
                "leadId", leadId,
                "score", 86,
                "grade", "A",
                "reason", "Industry, company scale, and recent intent signals match the configured ICP."));
    }

    @PostMapping("/leads/{leadId}/pitch")
    public ApiResponse<Map<String, Object>> generatePitch(@PathVariable Long leadId) {
        return ApiResponse.success(Map.of(
                "leadId", leadId,
                "channel", "CALL",
                "content", "I noticed your team appears to be expanding sales operations. We help B2B teams prioritize high-fit leads and improve follow-up conversion."));
    }
}
