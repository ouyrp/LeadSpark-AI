package com.leadspark.analytics;

import com.leadspark.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {
    @GetMapping("/workbench")
    public ApiResponse<Map<String, Object>> workbench() {
        return ApiResponse.success(Map.of(
                "todayTasks", 12,
                "overdueTasks", 2,
                "newHighScoreLeads", 8,
                "weeklyTouches", 46,
                "monthlyDealAmount", 128000));
    }
}
