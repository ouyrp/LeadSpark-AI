package com.leadspark.importexport;

import com.leadspark.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/import-tasks")
@Profile("!test")
public class ImportTaskController {
    private final LeadImportService leadImportService;

    public ImportTaskController(LeadImportService leadImportService) {
        this.leadImportService = leadImportService;
    }

    @PostMapping("/leads")
    public ApiResponse<Map<String, Object>> createLeadImportTask(
            @Valid @RequestBody LeadImportService.LeadImportRequest request) {
        return ApiResponse.success(leadImportService.importRows(
                request.rows() == null ? List.of() : request.rows(),
                request.sourceType() == null || request.sourceType().isBlank() ? "MANUAL_IMPORT" : request.sourceType(),
                request.fileName()));
    }

    @PostMapping("/competitor-signals")
    public ApiResponse<Map<String, Object>> importCompetitorSignals(
            @RequestBody(required = false) LeadImportService.CompetitorSignalImportRequest request) {
        LeadImportService.CompetitorSignalImportRequest actualRequest = request == null
                ? new LeadImportService.CompetitorSignalImportRequest(100, 0)
                : request;
        return ApiResponse.success(leadImportService.importCompetitorSignals(actualRequest));
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.success(leadImportService.recentTasks());
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.success(leadImportService.task(id));
    }

    @GetMapping("/{id}/errors")
    public ApiResponse<List<Map<String, Object>>> errors(@PathVariable Long id) {
        return ApiResponse.success(leadImportService.taskErrors(id));
    }
}
