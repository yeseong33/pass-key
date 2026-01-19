# Passkey 인증 프로젝트

## 프로젝트 개요
iOS 앱에서 Passkey(FIDO2/WebAuthn) 인증을 구현하는 프로젝트

## 기술 스택

### 백엔드 (Server)
- **언어**: Java 17+
- **프레임워크**: Spring Boot 3.x
- **라이브러리**: WebAuthn4J (Passkey 처리)
- **데이터베이스**: H2 (개발용) → MySQL/PostgreSQL (운영)

### 프론트엔드 (iOS App)
- **언어**: Swift 5.x
- **프레임워크**: AuthenticationServices (ASAuthorizationController)
- **최소 지원**: iOS 16+ (Passkey 지원 필수)

## 프로젝트 구조
```
passkey/
├── docs/                    # 문서 폴더
│   ├── 01_project_overview.md
│   ├── 02_server_setup.md
│   ├── 03_ios_setup.md
│   └── 04_api_spec.md
├── server/                  # Spring Boot 백엔드
│   └── (Java 소스)
└── ios-app/                 # iOS 앱
    └── (Swift 소스)
```

## Passkey 인증 흐름

### 1. 등록 (Registration)
```
[iOS App] → 등록 요청 → [Server]
[Server] → challenge 생성 → [iOS App]
[iOS App] → Passkey 생성 (Face ID/Touch ID) → 공개키 전송 → [Server]
[Server] → 공개키 저장 → 등록 완료
```

### 2. 인증 (Authentication)
```
[iOS App] → 인증 요청 → [Server]
[Server] → challenge 생성 → [iOS App]
[iOS App] → Passkey 서명 (Face ID/Touch ID) → 서명 전송 → [Server]
[Server] → 서명 검증 → 인증 완료
```

## 진행 상태
- [x] 프로젝트 구조 설계
- [x] 서버 프로젝트 생성 (Spring Boot + WebAuthn4J)
- [x] Passkey 등록/인증 API 구현
- [x] API 명세서 작성
- [x] iOS 클라이언트 가이드 작성
- [ ] iOS 앱 프로젝트 생성 (Xcode에서 직접 생성 필요)
- [ ] iOS 앱 연동 테스트
