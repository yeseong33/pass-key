package com.example.passkey.global.captcha;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Google reCAPTCHA API 호출을 담당하는 클라이언트
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecaptchaClient {

    private final WebClient recaptchaWebClient;

    /**
     * Google reCAPTCHA API를 호출하여 토큰을 검증합니다.ㄷ
     *
     * @param token     클라이언트에서 받은 reCAPTCHA 토큰
     * @param secretKey reCAPTCHA secret key (v2 또는 v3)
     * @return Google API 응답
     */
    public RecaptchaResponse verify(String token, String secretKey) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("secret", secretKey);
        formData.add("response", token);

        log.debug("Calling Google reCAPTCHA API");

        return recaptchaWebClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(RecaptchaResponse.class)
                .block();
    }
}
