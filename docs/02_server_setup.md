# Passkey 서버 설정 가이드

## 프로젝트 구조

```
server/
├── build.gradle                 # Gradle 빌드 설정
├── settings.gradle
└── src/main/
    ├── java/com/example/passkey/
    │   ├── PasskeyApplication.java       # Spring Boot 메인
    │   ├── config/
    │   │   └── WebAuthnConfig.java       # WebAuthn 설정
    │   ├── controller/
    │   │   └── PasskeyController.java    # REST API
    │   ├── domain/
    │   │   ├── User.java                 # 사용자 엔티티
    │   │   └── Credential.java           # Passkey 저장
    │   ├── dto/
    │   │   ├── RegistrationStartRequest.java
    │   │   ├── RegistrationStartResponse.java
    │   │   ├── RegistrationFinishRequest.java
    │   │   ├── AuthenticationStartRequest.java
    │   │   ├── AuthenticationStartResponse.java
    │   │   └── AuthenticationFinishRequest.java
    │   ├── repository/
    │   │   ├── UserRepository.java
    │   │   └── CredentialRepository.java
    │   └── service/
    │       ├── PasskeyService.java       # 핵심 비즈니스 로직
    │       └── ChallengeService.java     # Challenge 임시 저장
    └── resources/
        └── application.yml               # 애플리케이션 설정

```

## 실행 방법

### 1. 필수 요구사항
- Java 17 이상
- Gradle 8.x

### 2. 서버 실행
```bash
cd server
./gradlew bootRun
```

### 3. 확인
- 서버: http://localhost:8080
- H2 콘솔: http://localhost:8080/h2-console
- 헬스체크: http://localhost:8080/api/passkey/health

## API 엔드포인트

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/passkey/register/start` | 등록 시작 (challenge 발급) |
| POST | `/api/passkey/register/finish` | 등록 완료 (credential 저장) |
| POST | `/api/passkey/authenticate/start` | 인증 시작 (challenge 발급) |
| POST | `/api/passkey/authenticate/finish` | 인증 완료 (서명 검증) |
| GET | `/api/passkey/health` | 헬스 체크 |

## 주요 의존성

- **Spring Boot 3.2.1**: 웹 프레임워크
- **WebAuthn4J 0.22.1**: Passkey(WebAuthn) 처리 라이브러리
- **H2 Database**: 개발용 인메모리 DB
- **Lombok**: 보일러플레이트 코드 감소

## 설정 변경

`application.yml`에서 설정 변경 가능:

```yaml
webauthn:
  rp:
    id: localhost                    # 도메인 (iOS에서는 실제 도메인 필요)
    name: Passkey Demo               # 표시 이름
    origin: http://localhost:8080    # 허용 Origin
```

## iOS 연동 시 주의사항

1. **HTTPS 필수**: iOS는 localhost 제외하고 HTTPS만 허용
2. **도메인 설정**: `rp.id`가 실제 도메인과 일치해야 함
3. **Associated Domains**: iOS 앱에 `webcredentials:도메인` 설정 필요
