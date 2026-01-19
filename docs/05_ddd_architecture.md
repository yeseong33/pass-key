# DDD 아키텍처 구조

## 개요

Passkey 프로젝트는 Domain-Driven Design (DDD) 패턴을 적용하여 도메인 중심의 아키텍처로 구성되었습니다.

## 패키지 구조

```
com.example.passkey/
├── PasskeyApplication.java
├── global/
│   └── config/
│       ├── WebAuthnConfig.java
│       └── SwaggerConfig.java
└── domain/
    ├── user/
    │   ├── entity/
    │   │   └── User.java
    │   └── repository/
    │       └── UserRepository.java
    ├── credential/
    │   ├── entity/
    │   │   └── Credential.java
    │   └── repository/
    │       └── CredentialRepository.java
    └── auth/
        ├── controller/
        │   └── AuthController.java
        ├── service/
        │   ├── AuthService.java
        │   └── ChallengeService.java
        └── dto/
            ├── request/
            │   ├── RegistrationStartRequest.java
            │   ├── RegistrationFinishRequest.java
            │   ├── AuthenticationStartRequest.java
            │   └── AuthenticationFinishRequest.java
            └── response/
                ├── RegistrationStartResponse.java
                └── AuthenticationStartResponse.java
```

## 도메인 설명

### 1. User 도메인 (`domain/user/`)

사용자 관리를 담당하는 도메인입니다.

| 컴포넌트 | 파일 | 설명 |
|----------|------|------|
| Entity | `User.java` | 사용자 엔티티. username, displayName, credentials 관리 |
| Repository | `UserRepository.java` | 사용자 조회 및 저장 |

### 2. Credential 도메인 (`domain/credential/`)

Passkey 자격증명을 관리하는 도메인입니다.

| 컴포넌트 | 파일 | 설명 |
|----------|------|------|
| Entity | `Credential.java` | 패스키 자격증명 엔티티. credentialId, publicKey, signCount 관리 |
| Repository | `CredentialRepository.java` | 자격증명 조회 및 저장 |

### 3. Auth 도메인 (`domain/auth/`)

WebAuthn 인증 프로세스를 담당하는 도메인입니다.

| 컴포넌트 | 파일 | 설명 |
|----------|------|------|
| Controller | `AuthController.java` | REST API 엔드포인트 제공 |
| Service | `AuthService.java` | 등록/인증 비즈니스 로직 |
| Service | `ChallengeService.java` | Challenge 저장 및 관리 |
| DTO | `request/*.java` | 요청 데이터 전송 객체 |
| DTO | `response/*.java` | 응답 데이터 전송 객체 |

### 4. Global (`global/`)

전역 설정을 담당하는 패키지입니다.

| 컴포넌트 | 파일 | 설명 |
|----------|------|------|
| Config | `WebAuthnConfig.java` | WebAuthn 설정 (RP ID, Origin 등) |
| Config | `SwaggerConfig.java` | Swagger/OpenAPI 설정 |

## API 엔드포인트

기존 `/api/passkey`에서 `/api/auth`로 변경되었습니다.

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/auth/register/start` | 등록 시작 (Challenge 발급) |
| POST | `/api/auth/register/finish` | 등록 완료 (Credential 저장) |
| POST | `/api/auth/authenticate/start` | 인증 시작 (Challenge 발급) |
| POST | `/api/auth/authenticate/finish` | 인증 완료 (서명 검증) |
| GET | `/api/auth/health` | 헬스 체크 |

## 의존성 흐름

```
Controller → Service → Repository → Entity
     ↓
    DTO
```

- **Controller**: 외부 요청 수신, DTO 변환, Service 호출
- **Service**: 비즈니스 로직 처리, 트랜잭션 관리
- **Repository**: 데이터 영속성 관리
- **Entity**: 도메인 모델 정의

## 변경 이력

### v1.0.0 (리팩토링)

기존 레이어드 아키텍처에서 DDD 구조로 리팩토링:

| 변경 전 | 변경 후 |
|---------|---------|
| `config/` | `global/config/` |
| `domain/User.java` | `domain/user/entity/User.java` |
| `domain/Credential.java` | `domain/credential/entity/Credential.java` |
| `repository/` | `domain/{user,credential}/repository/` |
| `dto/` | `domain/auth/dto/{request,response}/` |
| `service/PasskeyService.java` | `domain/auth/service/AuthService.java` |
| `service/ChallengeService.java` | `domain/auth/service/ChallengeService.java` |
| `controller/PasskeyController.java` | `domain/auth/controller/AuthController.java` |

## 헬스 체크

```bash
curl http://localhost:8080/api/auth/health
```

예상 응답:
```json
{"status": "ok"}
```
