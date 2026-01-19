package com.example.passkey.domain.auth.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record AuthenticationFinishRequest(
        @NotBlank String id,      // credentialId (Base64URL)
        @NotBlank String rawId,   // credentialId raw (Base64URL)
        @Valid AuthenticatorAssertionResponse response,
        @NotBlank String type     // "public-key"
) {
    public record AuthenticatorAssertionResponse(
            String clientDataJSON,     // Base64URL
            String authenticatorData,  // Base64URL
            String signature,          // Base64URL
            String userHandle          // Base64URL (optional)
    ) {}
}
