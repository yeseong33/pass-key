package com.example.passkey.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RegistrationStartRequest(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Display name is required")
        String displayName
) {
}
