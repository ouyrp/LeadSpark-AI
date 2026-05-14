package com.leadspark.crm;

import com.leadspark.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/opportunities")
public class OpportunityController {
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.success(List.of(Map.of(
                "id", 1,
                "companyName", "LeadSpark Demo Co.",
                "stage", "QUALIFIED",
                "amount", new BigDecimal("68000.00"),
                "probability", 60)));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create() {
        return ApiResponse.success(Map.of("id", 10001, "stage", "QUALIFIED", "status", "OPEN"));
    }
}
