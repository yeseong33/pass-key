package com.example.passkey.global.exception;

import com.example.passkey.global.captcha.CaptchaException;
import com.example.passkey.global.captcha.RequireV2CaptchaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RequireV2CaptchaException.class)
    public ResponseEntity<Map<String, Object>> handleRequireV2Captcha(RequireV2CaptchaException e) {
        log.info("reCAPTCHA v2 required");
        return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED)
                .body(Map.of("requireV2Captcha", true));
    }

    @ExceptionHandler(CaptchaException.class)
    public ResponseEntity<Map<String, Object>> handleCaptchaException(CaptchaException e) {
        log.warn("CAPTCHA verification failed: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "error", "CAPTCHA_FAILED",
                        "message", e.getMessage()
                ));
    }
}
