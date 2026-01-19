package com.example.passkey.domain.auth.service;

import com.example.passkey.global.config.WebAuthnConfig;
import com.example.passkey.domain.credential.entity.Credential;
import com.example.passkey.domain.user.entity.User;
import com.example.passkey.domain.auth.dto.request.*;
import com.example.passkey.domain.auth.dto.response.*;
import com.example.passkey.domain.credential.repository.CredentialRepository;
import com.example.passkey.domain.user.repository.UserRepository;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.authenticator.AuthenticatorImpl;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.*;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.server.ServerProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final WebAuthnManager webAuthnManager;
    private final WebAuthnConfig webAuthnConfig;
    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final ChallengeService challengeService;
    private final ObjectConverter objectConverter = new ObjectConverter();

    /**
     * 등록 시작: challenge 생성 및 옵션 반환
     */
    @Transactional
    public RegistrationStartResponse startRegistration(RegistrationStartRequest request) {
        // 사용자 생성 또는 조회
        User user = userRepository.findByUsername(request.username())
                .orElseGet(() -> {
                    User newUser = new User(request.username(), request.displayName());
                    return userRepository.save(newUser);
                });

        // Challenge 생성 및 저장
        byte[] challenge = webAuthnConfig.generateChallenge().getValue();
        challengeService.storeChallenge(request.username(), challenge);

        // 응답 생성
        return new RegistrationStartResponse(
                Base64.getUrlEncoder().withoutPadding().encodeToString(challenge),
                new RegistrationStartResponse.RelyingParty(
                        webAuthnConfig.getRpId(),
                        webAuthnConfig.getRpName()
                ),
                new RegistrationStartResponse.UserInfo(
                        Base64.getUrlEncoder().withoutPadding().encodeToString(user.getId().toString().getBytes()),
                        user.getUsername(),
                        user.getDisplayName()
                ),
                List.of(
                        new RegistrationStartResponse.PubKeyCredParam("public-key", -7),  // ES256
                        new RegistrationStartResponse.PubKeyCredParam("public-key", -257)  // RS256
                ),
                60000L,
                "none"
        );
    }

    /**
     * 등록 완료: 클라이언트 응답 검증 및 credential 저장
     */
    @Transactional
    public void finishRegistration(RegistrationFinishRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("User not found"));

        byte[] challenge = challengeService.getAndRemoveChallenge(request.username());
        if (challenge == null) {
            throw new RuntimeException("Challenge not found or expired");
        }

        // Base64URL 디코딩
        byte[] clientDataJSON = Base64.getUrlDecoder().decode(request.response().clientDataJSON());
        byte[] attestationObject = Base64.getUrlDecoder().decode(request.response().attestationObject());

        // 서버 속성 생성
        ServerProperty serverProperty = webAuthnConfig.createServerProperty(challenge);

        // 등록 데이터 생성
        RegistrationRequest registrationRequest = new RegistrationRequest(
                attestationObject,
                clientDataJSON
        );

        RegistrationParameters registrationParameters = new RegistrationParameters(
                serverProperty,
                null,  // pubKeyCredParams
                false  // userVerificationRequired
        );

        // WebAuthn 검증
        RegistrationData registrationData = webAuthnManager.parse(registrationRequest);
        webAuthnManager.verify(registrationData, registrationParameters);

        // Credential 저장
        AttestedCredentialData attestedCredentialData = registrationData
                .getAttestationObject()
                .getAuthenticatorData()
                .getAttestedCredentialData();

        byte[] credentialIdBytes = attestedCredentialData.getCredentialId();
        String credentialId = Base64.getUrlEncoder().withoutPadding().encodeToString(credentialIdBytes);

        // 공개키 직렬화
        COSEKey coseKey = attestedCredentialData.getCOSEKey();
        byte[] publicKeyBytes = objectConverter.getCborConverter().writeValueAsBytes(coseKey);

        String aaguid = attestedCredentialData.getAaguid() != null
                ? attestedCredentialData.getAaguid().toString()
                : null;

        Credential credential = new Credential(
                credentialId,
                publicKeyBytes,
                registrationData.getAttestationObject().getAuthenticatorData().getSignCount(),
                aaguid
        );

        user.addCredential(credential);
        credentialRepository.save(credential);

        log.info("Passkey registered for user: {}", user.getUsername());
    }

    /**
     * 인증 시작: challenge 생성
     */
    @Transactional(readOnly = true)
    public AuthenticationStartResponse startAuthentication(AuthenticationStartRequest request) {
        // Challenge 생성
        byte[] challenge = webAuthnConfig.generateChallenge().getValue();

        List<AuthenticationStartResponse.AllowCredential> allowCredentials;
        String challengeKey;

        if (request.username() != null && !request.username().isEmpty()) {
            // 특정 사용자의 credentials 조회
            User user = userRepository.findByUsername(request.username())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            allowCredentials = user.getCredentials().stream()
                    .map(c -> new AuthenticationStartResponse.AllowCredential("public-key", c.getCredentialId()))
                    .collect(Collectors.toList());

            challengeKey = request.username();
        } else {
            // Discoverable credential 사용 (allowCredentials 비움)
            allowCredentials = Collections.emptyList();
            challengeKey = "anonymous_" + System.currentTimeMillis();
        }

        challengeService.storeChallenge(challengeKey, challenge);

        return new AuthenticationStartResponse(
                Base64.getUrlEncoder().withoutPadding().encodeToString(challenge),
                60000L,
                webAuthnConfig.getRpId(),
                allowCredentials,
                "preferred"
        );
    }

    /**
     * 인증 완료: 서명 검증
     */
    @Transactional
    public User finishAuthentication(AuthenticationFinishRequest request) {
        // Credential 조회
        Credential credential = credentialRepository.findByCredentialId(request.id())
                .orElseThrow(() -> new RuntimeException("Credential not found"));

        User user = credential.getUser();

        byte[] challenge = challengeService.getAndRemoveChallenge(user.getUsername());
        if (challenge == null) {
            throw new RuntimeException("Challenge not found or expired");
        }

        // Base64URL 디코딩
        byte[] credentialId = Base64.getUrlDecoder().decode(request.id());
        byte[] clientDataJSON = Base64.getUrlDecoder().decode(request.response().clientDataJSON());
        byte[] authenticatorData = Base64.getUrlDecoder().decode(request.response().authenticatorData());
        byte[] signature = Base64.getUrlDecoder().decode(request.response().signature());

        // 서버 속성 생성
        ServerProperty serverProperty = webAuthnConfig.createServerProperty(challenge);

        // 인증 데이터 생성
        AuthenticationRequest authenticationRequest = new AuthenticationRequest(
                credentialId,
                null,  // userHandle
                authenticatorData,
                clientDataJSON,
                null,  // clientExtensionJSON
                signature
        );

        AuthenticationParameters authenticationParameters = new AuthenticationParameters(
                serverProperty,
                buildAuthenticator(credential),
                null,  // allowCredentials
                false  // userVerificationRequired
        );

        // WebAuthn 검증
        AuthenticationData authenticationData = webAuthnManager.parse(authenticationRequest);
        webAuthnManager.verify(authenticationData, authenticationParameters);

        // Sign count 업데이트
        credential.updateSignCount(authenticationData.getAuthenticatorData().getSignCount());
        credentialRepository.save(credential);

        log.info("User authenticated: {}", user.getUsername());
        return user;
    }

    private Authenticator buildAuthenticator(Credential credential) {
        COSEKey coseKey = objectConverter.getCborConverter()
                .readValue(credential.getPublicKey(), COSEKey.class);

        // AAGUID 생성 (저장된 값이 없으면 ZERO 사용)
        AAGUID aaguid;
        if (credential.getAaguid() != null && !credential.getAaguid().isEmpty()) {
            aaguid = new AAGUID(credential.getAaguid());
        } else {
            aaguid = AAGUID.ZERO;
        }

        // AttestedCredentialData 생성
        AttestedCredentialData attestedCredentialData = new AttestedCredentialData(
                aaguid,
                Base64.getUrlDecoder().decode(credential.getCredentialId()),
                coseKey
        );

        return new AuthenticatorImpl(
                attestedCredentialData,
                null,  // attestationStatement
                credential.getSignCount()
        );
    }
}
