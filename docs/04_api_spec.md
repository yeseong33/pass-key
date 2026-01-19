# Passkey API 명세서

> **Swagger UI**: http://localhost:8080/swagger-ui.html
> **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## Base URL
```
http://localhost:8080/api/passkey
```

---

## 1. 등록 시작 (Registration Start)

Passkey 등록을 시작하고 challenge를 발급받습니다.

### Request
```
POST /register/start
Content-Type: application/json
```

```json
{
  "username": "user@example.com",
  "displayName": "홍길동"
}
```

### Response
```json
{
  "challenge": "dGVzdC1jaGFsbGVuZ2U...",
  "rp": {
    "id": "localhost",
    "name": "Passkey Demo"
  },
  "user": {
    "id": "dXNlci1pZC0xMjM...",
    "name": "user@example.com",
    "displayName": "홍길동"
  },
  "pubKeyCredParams": [
    { "type": "public-key", "alg": -7 },
    { "type": "public-key", "alg": -257 }
  ],
  "timeout": 60000,
  "attestation": "none"
}
```

---

## 2. 등록 완료 (Registration Finish)

클라이언트에서 생성한 credential을 서버에 저장합니다.

### Request
```
POST /register/finish
Content-Type: application/json
```

```json
{
  "username": "user@example.com",
  "id": "credential-id-base64url",
  "rawId": "credential-id-base64url",
  "type": "public-key",
  "response": {
    "clientDataJSON": "eyJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIi...",
    "attestationObject": "o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRoRGF0YVi..."
  }
}
```

### Response (성공)
```json
{
  "success": true,
  "message": "Passkey registered successfully"
}
```

---

## 3. 인증 시작 (Authentication Start)

Passkey 인증을 시작하고 challenge를 발급받습니다.

### Request
```
POST /authenticate/start
Content-Type: application/json
```

```json
{
  "username": "user@example.com"
}
```

> username이 null이면 Discoverable Credential 사용 (모든 등록된 Passkey 허용)

### Response
```json
{
  "challenge": "YXV0aC1jaGFsbGVuZ2U...",
  "timeout": 60000,
  "rpId": "localhost",
  "allowCredentials": [
    {
      "type": "public-key",
      "id": "credential-id-base64url"
    }
  ],
  "userVerification": "preferred"
}
```

---

## 4. 인증 완료 (Authentication Finish)

서명을 검증하고 인증을 완료합니다.

### Request
```
POST /authenticate/finish
Content-Type: application/json
```

```json
{
  "id": "credential-id-base64url",
  "rawId": "credential-id-base64url",
  "type": "public-key",
  "response": {
    "clientDataJSON": "eyJ0eXBlIjoid2ViYXV0aG4uZ2V0Ii...",
    "authenticatorData": "SZYN5YgOjGh0NBcPZHZgW4_krrmihjLHmVzzuoMdl2MFAAAAAA...",
    "signature": "MEUCIQDx...",
    "userHandle": "dXNlci1pZC0xMjM..."
  }
}
```

### Response (성공)
```json
{
  "success": true,
  "message": "Authentication successful",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "user@example.com"
}
```

---

## 5. 헬스 체크

### Request
```
GET /health
```

### Response
```json
{
  "status": "ok"
}
```

---

## 에러 응답

### 형식
```json
{
  "timestamp": "2024-01-15T10:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Challenge not found or expired",
  "path": "/api/passkey/register/finish"
}
```

### 주요 에러 코드
| Status | 설명 |
|--------|------|
| 400 | 잘못된 요청 (Challenge 만료, 잘못된 형식 등) |
| 404 | 사용자 또는 Credential을 찾을 수 없음 |
| 500 | 서버 내부 오류 |

---

## 인코딩 규칙

- **Challenge, Credential ID**: Base64URL (패딩 없음)
- **clientDataJSON, attestationObject**: Base64URL
- **User ID**: UUID → bytes → Base64URL
