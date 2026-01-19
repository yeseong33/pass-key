package com.example.passkey.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Passkey API")
                        .description("Passkey(WebAuthn/FIDO2) 인증 서버 API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Passkey Server")
                                .email("admin@example.com")));
    }
}
