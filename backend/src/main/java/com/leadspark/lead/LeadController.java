package com.leadspark.lead;

import com.leadspark.common.api.ApiResponse;
import com.leadspark.common.api.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/leads")
public class LeadController {
    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {
        List<Map<String, Object>> items = List.of(Map.of(
                "id", 1,
                "companyName", "LeadSpark Demo Co.",
                "industry", "B2B SaaS",
                "region", "Shanghai",
                "status", "NEW",
                "score", 86,
                "grade", "A",
                "createdAt", Instant.now().toString()));
        return ApiResponse.success(new PageResult<>(items, page, pageSize, items.size()));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.success(Map.of(
                "id", id,
                "companyName", "LeadSpark Demo Co.",
                "status", "NEW",
                "score", 86,
                "scoreReason", "ICP matched with clear expansion signal.",
                "recommendedChannel", "CALL"));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateLeadRequest request) {
        return ApiResponse.success(Map.of(
                "id", 10001,
                "companyName", request.companyName(),
                "status", "NEW"));
    }

    public record CreateLeadRequest(@NotBlank String companyName, String source, String industry, String region) {
    }
}
