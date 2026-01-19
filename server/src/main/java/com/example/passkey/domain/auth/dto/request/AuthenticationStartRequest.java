package com.example.passkey.domain.auth.dto.request;

public record AuthenticationStartRequest(
        String username  // Optional: null이면 discoverable credential 사용
) {
}
