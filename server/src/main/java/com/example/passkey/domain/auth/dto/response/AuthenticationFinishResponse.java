package com.example.passkey.domain.auth.dto.response;

public record AuthenticationFinishResponse(
    boolean success,
    String message,
    String userId,
    String username
) {

    public static AuthenticationFinishResponse success(String userId, String username) {
        return new AuthenticationFinishResponse(true, "Authentication successful", userId,
            username);
    }
}
