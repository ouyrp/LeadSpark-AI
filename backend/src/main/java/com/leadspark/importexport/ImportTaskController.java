package com.leadspark.importexport;

import com.leadspark.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/import-tasks")
public class ImportTaskController {
    @PostMapping("/leads")
    public ApiResponse<Map<String, Object>> createLeadImportTask() {
        return ApiResponse.success(Map.of(
                "taskId", 10001,
                "status", "PENDING",
                "createdAt", Instant.now().toString()));
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.success(List.of(Map.of(
                "taskId", 10001,
                "type", "LEAD_IMPORT",
                "status", "SUCCESS",
                "totalRows", 120,
                "successRows", 116,
                "failedRows", 4)));
    }
}
