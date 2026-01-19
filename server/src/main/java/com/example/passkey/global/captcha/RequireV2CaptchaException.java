package com.example.passkey.global.captcha;

/**
 * reCAPTCHA v3 점수가 낮아 v2 검증이 필요할 때 발생하는 예외
 */
public class RequireV2CaptchaException extends RuntimeException {

    public RequireV2CaptchaException() {
        super("reCAPTCHA v2 verification required");
    }
}
