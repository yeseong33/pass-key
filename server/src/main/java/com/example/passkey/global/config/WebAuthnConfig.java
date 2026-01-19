package com.example.passkey.global.config;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;

@Configuration
public class WebAuthnConfig {

    @Value("${webauthn.rp.id}")
    private String rpId;

    @Value("${webauthn.rp.name}")
    private String rpName;

    @Value("${webauthn.rp.origin}")
    private String origin;

    @Bean
    public WebAuthnManager webAuthnManager() {
        return WebAuthnManager.createNonStrictWebAuthnManager();
    }

    @Bean
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }

    public String getRpId() {
        return rpId;
    }

    public String getRpName() {
        return rpName;
    }

    public Origin getOrigin() {
        return new Origin(origin);
    }

    public DefaultChallenge generateChallenge() {
        byte[] challengeBytes = new byte[32];
        secureRandom().nextBytes(challengeBytes);
        return new DefaultChallenge(challengeBytes);
    }

    public ServerProperty createServerProperty(byte[] challenge) {
        return new ServerProperty(
                getOrigin(),
                rpId,
                new DefaultChallenge(challenge),
                null  // tokenBinding
        );
    }
}
