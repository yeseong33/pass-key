package com.example.passkey.domain.auth.dto.response;

import java.util.List;

public record AuthenticationStartResponse(
    String challenge,  // Base64 encoded
    long timeout,
    String rpId,
    List<AllowCredential> allowCredentials,
    String userVerification
) {

    public record AllowCredential(
        String type,
        String id  // Base64URL encoded credentialId
    ) {

    }
}
