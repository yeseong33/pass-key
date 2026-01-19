package com.example.passkey.domain.auth.dto.response;

import java.util.List;

public record RegistrationStartResponse(
    String challenge,  // Base64 encoded
    RelyingParty rp,
    UserInfo user,
    List<PubKeyCredParam> pubKeyCredParams,
    long timeout,
    String attestation
) {

    public record RelyingParty(
        String id,
        String name
    ) {

    }

    public record UserInfo(
        String id,  // Base64 encoded user handle
        String name,
        String displayName
    ) {

    }

    public record PubKeyCredParam(
        String type,
        int alg
    ) {

    }
}
