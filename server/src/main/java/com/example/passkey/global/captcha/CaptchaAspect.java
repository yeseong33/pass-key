package com.example.passkey.global.captcha;

import com.example.passkey.global.captcha.RecaptchaService.CaptchaResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class CaptchaAspect {

    private static final String CAPTCHA_V3_HEADER = "X-Captcha-Token";
    private static final String CAPTCHA_V2_HEADER = "X-Captcha-Token-V2";

    private final RecaptchaService recaptchaService;

    @Before("@annotation(requireCaptcha)")
    public void verifyCaptcha(JoinPoint joinPoint, RequireCaptcha requireCaptcha) {
        HttpServletRequest request = getCurrentHttpRequest();

        String v3Token = request.getHeader(CAPTCHA_V3_HEADER);
        String v2Token = request.getHeader(CAPTCHA_V2_HEADER);

        log.debug("Verifying CAPTCHA for method: {}, v3Token: {}, v2Token: {}",
                joinPoint.getSignature().getName(),
                v3Token != null ? "present" : "null",
                v2Token != null ? "present" : "null");

        CaptchaResult result = recaptchaService.verify(v3Token, v2Token);

        if (result == CaptchaResult.REQUIRE_V2) {
            throw new RequireV2CaptchaException();
        }
    }

    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }
}
