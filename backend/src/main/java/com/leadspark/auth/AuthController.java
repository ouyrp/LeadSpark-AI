package com.leadspark.auth;

import com.leadspark.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(Map.of(
                "accessToken", "dev-access-token",
                "refreshToken", "dev-refresh-token",
                "expiresIn", 7200,
                "user", Map.of(
                        "id", 1001,
                        "name", "Demo Sales",
                        "roles", List.of("TENANT_ADMIN"))));
    }

    public record LoginRequest(@NotBlank String account, @NotBlank String password) {
    }
}
