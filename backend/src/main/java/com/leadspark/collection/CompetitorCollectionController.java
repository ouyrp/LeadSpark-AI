package com.leadspark.collection;

import com.leadspark.common.api.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/competitor-collections")
@Profile("!test")
public class CompetitorCollectionController {
    private final CompetitorCollectionProperties properties;
    private final CompetitorCollectionService collectionService;

    public CompetitorCollectionController(
            CompetitorCollectionProperties properties,
            CompetitorCollectionService collectionService) {
        this.properties = properties;
        this.collectionService = collectionService;
    }

    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> config() {
        return ApiResponse.success(Map.of(
                "enabled", properties.isEnabled(),
                "cron", properties.getCron(),
                "tenantId", properties.getTenantId(),
                "keywords", properties.getKeywords()));
    }

    @PostMapping("/run")
    public ApiResponse<Map<String, Object>> runManually() {
        return ApiResponse.success(collectionService.run("MANUAL"));
    }

    @GetMapping("/jobs")
    public ApiResponse<Map<String, Object>> jobs() {
        return ApiResponse.success(Map.of("items", collectionService.recentJobs()));
    }

    @GetMapping("/signals")
    public ApiResponse<Map<String, Object>> signals() {
        return ApiResponse.success(Map.of("items", collectionService.recentSignals()));
    }
}
