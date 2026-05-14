package com.leadspark.crm;

import com.leadspark.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/leads/{leadId}/follow-ups")
public class FollowUpController {
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@PathVariable Long leadId) {
        return ApiResponse.success(List.of(Map.of(
                "id", 1,
                "leadId", leadId,
                "channel", "CALL",
                "result", "INTERESTED",
                "content", "Customer asked for a product overview.",
                "createdAt", Instant.now().toString())));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@PathVariable Long leadId) {
        return ApiResponse.success(Map.of("id", 10001, "leadId", leadId));
    }
}
