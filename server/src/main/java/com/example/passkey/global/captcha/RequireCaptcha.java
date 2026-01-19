package com.example.passkey.global.captcha;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 이 어노테이션이 붙은 메서드는 reCAPTCHA 검증을 수행합니다.
 * 메서드의 파라미터 중 CaptchaTokenProvider를 구현한 객체에서 토큰을 추출합니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireCaptcha {
}
