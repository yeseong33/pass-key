package com.example.passkey.global.captcha;

import com.example.passkey.global.config.RecaptchaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * reCAPTCHA 검증 비즈니스 로직을 담당하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecaptchaService {

    private final RecaptchaConfig recaptchaConfig;
    private final RecaptchaClient recaptchaClient;

    public enum CaptchaResult {
        PASS,
        REQUIRE_V2,
        FAIL
    }

    public CaptchaResult verify(String v3Token, String v2Token) {
        if (!recaptchaConfig.isEnabled()) {
            log.debug("reCAPTCHA is disabled, skipping verification");
            return CaptchaResult.PASS;
        }

        // v2 토큰이 있으면 v2 검증 우선
        if (StringUtils.hasText(v2Token)) {
            return verifyV2(v2Token);
        }

        // v3 토큰 검증
        if (StringUtils.hasText(v3Token)) {
            return verifyV3(v3Token);
        }

        log.warn("No CAPTCHA token provided");
        throw new CaptchaException("CAPTCHA token is required");
    }

    private CaptchaResult verifyV3(String token) {
        try {
            RecaptchaResponse response = recaptchaClient.verify(
                    token,
                    recaptchaConfig.getV3().getSecretKey()
            );

            if (!response.isValid()) {
                log.warn("reCAPTCHA v3 verification failed: {}", response.errorCodes());
                throw new CaptchaException("reCAPTCHA verification failed");
            }

            double threshold = recaptchaConfig.getV3().getThreshold();
            if (response.meetsThreshold(threshold)) {
                log.info("reCAPTCHA v3 passed with score: {}", response.score());
                return CaptchaResult.PASS;
            }

            log.info("reCAPTCHA v3 score {} below threshold {}, requiring v2",
                    response.score(), threshold);
            return CaptchaResult.REQUIRE_V2;

        } catch (CaptchaException e) {
            throw e;
        } catch (Exception e) {
            log.error("reCAPTCHA v3 verification error", e);
            throw new CaptchaException("reCAPTCHA verification failed", e);
        }
    }

    private CaptchaResult verifyV2(String token) {
        try {
            RecaptchaResponse response = recaptchaClient.verify(
                    token,
                    recaptchaConfig.getV2().getSecretKey()
            );

            if (response.isValid()) {
                log.info("reCAPTCHA v2 verification passed");
                return CaptchaResult.PASS;
            }

            log.warn("reCAPTCHA v2 verification failed: {}", response.errorCodes());
            throw new CaptchaException("reCAPTCHA v2 verification failed");

        } catch (CaptchaException e) {
            throw e;
        } catch (Exception e) {
            log.error("reCAPTCHA v2 verification error", e);
            throw new CaptchaException("reCAPTCHA verification failed", e);
        }
    }
}
