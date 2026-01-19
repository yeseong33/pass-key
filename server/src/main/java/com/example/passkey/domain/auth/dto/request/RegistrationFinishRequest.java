package com.example.passkey.domain.auth.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record RegistrationFinishRequest(
        @NotBlank String username,
        @NotBlank String id,      // credentialId (Base64URL)
        @NotBlank String rawId,   // credentialId raw (Base64URL)
        @Valid AuthenticatorResponse response,
        @NotBlank String type     // "public-key"
) {
    public record AuthenticatorResponse(
            String clientDataJSON,    // Base64URL
            String attestationObject  // Base64URL
    ) {}
}
