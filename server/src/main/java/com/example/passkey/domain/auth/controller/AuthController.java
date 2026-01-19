package com.example.passkey.domain.auth.controller;

import com.example.passkey.domain.user.entity.User;
import com.example.passkey.domain.auth.dto.request.*;
import com.example.passkey.domain.auth.dto.response.*;
import com.example.passkey.domain.auth.service.AuthService;
import com.example.passkey.global.captcha.RequireCaptcha;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Auth", description = "Passkey(WebAuthn/FIDO2) 인증 API")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "등록 시작", description = "Passkey 등록을 시작하고 challenge를 발급받습니다")
    @PostMapping("/register/start")
    @RequireCaptcha
    public ResponseEntity<RegistrationStartResponse> startRegistration(
            @Valid @RequestBody RegistrationStartRequest request) {
        log.info("Registration start for user: {}", request.username());
        RegistrationStartResponse response = authService.startRegistration(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "등록 완료", description = "클라이언트에서 생성한 credential을 검증하고 저장합니다")
    @PostMapping("/register/finish")
    public ResponseEntity<RegistrationFinishResponse> finishRegistration(
            @Valid @RequestBody RegistrationFinishRequest request) {
        log.info("Registration finish for user: {}", request.username());
        authService.finishRegistration(request);
        return ResponseEntity.ok(RegistrationFinishResponse.ok());
    }

    @Operation(summary = "인증 시작", description = "Passkey 인증을 시작하고 challenge를 발급받습니다")
    @PostMapping("/authenticate/start")
    @RequireCaptcha
    public ResponseEntity<AuthenticationStartResponse> startAuthentication(
            @NotNull @RequestBody(required = false) AuthenticationStartRequest request) {
        log.info("Authentication start for user: {}", request != null ? request.username() : "null");
        AuthenticationStartResponse response = authService.startAuthentication(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "인증 완료", description = "서명을 검증하고 인증을 완료합니다")
    @PostMapping("/authenticate/finish")
    public ResponseEntity<AuthenticationFinishResponse> finishAuthentication(
            @Valid @RequestBody AuthenticationFinishRequest request) {
        log.info("Authentication finish for credentialId: {}", request.id());
        User user = authService.finishAuthentication(request);
        return ResponseEntity.ok(AuthenticationFinishResponse.success(
                user.getId().toString(),
                user.getUsername()
        ));
    }

    @Operation(summary = "헬스 체크", description = "서버 상태를 확인합니다")
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(HealthResponse.ok());
    }
}
