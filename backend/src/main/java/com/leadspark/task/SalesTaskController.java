package com.leadspark.task;

import com.leadspark.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
public class SalesTaskController {
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.success(List.of(Map.of(
                "id", 1,
                "leadId", 1,
                "taskType", "CALL",
                "status", "PENDING",
                "title", "Call high-score lead",
                "dueAt", Instant.now().toString())));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create() {
        return ApiResponse.success(Map.of("id", 10001, "status", "PENDING"));
    }
}
