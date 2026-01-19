package com.example.passkey.global.captcha;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public record RecaptchaResponse(
        boolean success,
        Double score,          // v3 only (0.0 ~ 1.0)
        String action,         // v3 only
        @JsonProperty("challenge_ts")
        Instant challengeTs,
        String hostname,
        @JsonProperty("error-codes")
        List<String> errorCodes
) {
    public boolean isValid() {
        return success;
    }

    public boolean meetsThreshold(double threshold) {
        return score != null && score >= threshold;
    }
}
