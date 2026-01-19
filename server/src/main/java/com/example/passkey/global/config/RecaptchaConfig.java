package com.example.passkey.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;

@Configuration
@ConfigurationProperties(prefix = "recaptcha")
@Getter
@Setter
public class RecaptchaConfig {

    private boolean enabled;
    private String verifyUrl;
    private V3Config v3 = new V3Config();
    private V2Config v2 = new V2Config();

    @Getter
    @Setter
    public static class V3Config {
        private String siteKey;
        private String secretKey;
        private double threshold = 0.5;
    }

    @Getter
    @Setter
    public static class V2Config {
        private String siteKey;
        private String secretKey;
    }

    @Bean
    public WebClient recaptchaWebClient() {
        // JDK HttpClient 사용 (macOS에서 Netty DNS resolver 문제 회피)
        HttpClient httpClient = HttpClient.newHttpClient();
        return WebClient.builder()
                .clientConnector(new JdkClientHttpConnector(httpClient))
                .baseUrl(verifyUrl)
                .build();
    }
}
