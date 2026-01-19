package com.example.passkey.domain.auth.dto.response;

public record HealthResponse(
        String status
) {
    public static HealthResponse ok() {
        return new HealthResponse("ok");
    }
}
