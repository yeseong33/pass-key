package com.example.passkey.domain.auth.dto.response;

public record RegistrationFinishResponse(
    boolean success,
    String message
) {

    public static RegistrationFinishResponse ok() {
        return new RegistrationFinishResponse(true, "Passkey registered successfully");
    }
}
