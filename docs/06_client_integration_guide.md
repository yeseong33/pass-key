# 클라이언트 통합 가이드

> 이 문서는 Passkey 서버를 사용하는 클라이언트 개발자를 위한 가이드입니다.
> 각 플랫폼(iOS, Android, Web)에서 서버에 전송해야 할 값들을 어떻게 생성하는지 설명합니다.

---

## 개요

Passkey 인증은 **등록(Registration)**과 **인증(Authentication)** 두 단계로 이루어집니다.
각 단계는 Start → Finish 형태로 서버와 2회 통신합니다.

```
[등록]
클라이언트 → POST /register/start (username, displayName)
서버 → challenge, user.id 등 반환
클라이언트 → Passkey 생성 (Face ID/Touch ID/Windows Hello 등)
클라이언트 → POST /register/finish (credential 정보)

[인증]
클라이언트 → POST /authenticate/start (username)
서버 → challenge 반환
클라이언트 → Passkey 서명 (Face ID/Touch ID/Windows Hello 등)
클라이언트 → POST /authenticate/finish (서명 정보)
```

---

## 1. 등록 (Registration)

### 1.1 등록 시작 요청

서버에 등록 시작을 요청하면 `challenge`와 `user.id`를 받습니다.

**요청:**
```json
POST /api/passkey/register/start
{
  "username": "user@example.com",
  "displayName": "홍길동"
}
```

**응답:**
```json
{
  "challenge": "dGVzdC1jaGFsbGVuZ2U...",  // Base64URL 인코딩
  "user": {
    "id": "dXNlci1pZC0xMjM...",           // Base64URL 인코딩
    "name": "user@example.com",
    "displayName": "홍길동"
  },
  "rp": {
    "id": "your-domain.com",
    "name": "Passkey Demo"
  },
  "pubKeyCredParams": [
    { "type": "public-key", "alg": -7 },    // ES256 (권장)
    { "type": "public-key", "alg": -257 }   // RS256
  ],
  "timeout": 60000,
  "attestation": "none"
}
```

### 1.2 클라이언트에서 Passkey 생성

서버 응답값을 사용해 플랫폼 API를 호출합니다.

#### iOS (AuthenticationServices)
```swift
let challenge = Data(base64URLDecoded: response.challenge)!
let userID = Data(base64URLDecoded: response.user.id)!

let provider = ASAuthorizationPlatformPublicKeyCredentialProvider(
    relyingPartyIdentifier: "your-domain.com"
)

let request = provider.createCredentialRegistrationRequest(
    challenge: challenge,
    name: response.user.name,
    userID: userID
)

let controller = ASAuthorizationController(authorizationRequests: [request])
controller.performRequests()
```

#### Android (FIDO2 API)
```kotlin
val options = PublicKeyCredentialCreationOptions.Builder()
    .setRp(PublicKeyCredentialRpEntity("your-domain.com", "Passkey Demo", null))
    .setUser(PublicKeyCredentialUserEntity(
        Base64.decode(response.user.id, Base64.URL_SAFE),
        response.user.name,
        null,
        response.user.displayName
    ))
    .setChallenge(Base64.decode(response.challenge, Base64.URL_SAFE))
    .setParameters(listOf(
        PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY.toString(), -7)
    ))
    .build()
```

#### Web (WebAuthn API)
```javascript
const options = {
    publicKey: {
        challenge: base64URLToBuffer(response.challenge),
        rp: {
            id: "your-domain.com",
            name: "Passkey Demo"
        },
        user: {
            id: base64URLToBuffer(response.user.id),
            name: response.user.name,
            displayName: response.user.displayName
        },
        pubKeyCredParams: [
            { type: "public-key", alg: -7 },
            { type: "public-key", alg: -257 }
        ],
        timeout: 60000,
        attestation: "none"
    }
};

const credential = await navigator.credentials.create(options);
```

### 1.3 등록 완료 요청 - 서버에 보낼 값 생성

Passkey 생성 후 플랫폼 API가 반환하는 값들을 **Base64URL 인코딩**하여 서버에 전송합니다.

#### 플랫폼별 반환값과 서버 전송 매핑

| 서버 필드 | iOS | Android | Web |
|-----------|-----|---------|-----|
| `id` | `credential.credentialID` | `response.keyHandle` | `credential.id` |
| `rawId` | `credential.credentialID` | `response.keyHandle` | `credential.rawId` |
| `response.clientDataJSON` | `credential.rawClientDataJSON` | `response.clientDataJSON` | `response.clientDataJSON` |
| `response.attestationObject` | `credential.rawAttestationObject` | `response.attestationObject` | `response.attestationObject` |

#### 서버에 보내는 요청 형식
```json
POST /api/passkey/register/finish
{
  "username": "user@example.com",
  "id": "<credentialID를 Base64URL 인코딩>",
  "rawId": "<credentialID를 Base64URL 인코딩>",
  "type": "public-key",
  "response": {
    "clientDataJSON": "<clientDataJSON 바이트를 Base64URL 인코딩>",
    "attestationObject": "<attestationObject 바이트를 Base64URL 인코딩>"
  }
}
```

#### clientDataJSON 구조 (참고용 - 자동 생성됨)
```json
{
  "type": "webauthn.create",
  "challenge": "dGVzdC1jaGFsbGVuZ2U...",
  "origin": "https://your-domain.com",
  "crossOrigin": false
}
```

#### attestationObject 구조 (참고용 - 자동 생성됨)
CBOR 인코딩된 데이터로, 다음을 포함:
- `fmt`: attestation 형식 ("none", "packed" 등)
- `attStmt`: attestation 증명 데이터
- `authData`: authenticator 데이터 (rpIdHash, flags, counter, 공개키 등)

---

## 2. 인증 (Authentication)

### 2.1 인증 시작 요청

**요청:**
```json
POST /api/passkey/authenticate/start
{
  "username": "user@example.com"  // null이면 Discoverable Credential 모드
}
```

**응답:**
```json
{
  "challenge": "YXV0aC1jaGFsbGVuZ2U...",
  "timeout": 60000,
  "rpId": "your-domain.com",
  "allowCredentials": [
    {
      "type": "public-key",
      "id": "<등록된 credential ID>"
    }
  ],
  "userVerification": "preferred"
}
```

### 2.2 클라이언트에서 Passkey 서명

#### iOS
```swift
let challenge = Data(base64URLDecoded: response.challenge)!

let provider = ASAuthorizationPlatformPublicKeyCredentialProvider(
    relyingPartyIdentifier: response.rpId
)

let request = provider.createCredentialAssertionRequest(
    challenge: challenge
)

let controller = ASAuthorizationController(authorizationRequests: [request])
controller.performRequests()
```

#### Android
```kotlin
val options = PublicKeyCredentialRequestOptions.Builder()
    .setChallenge(Base64.decode(response.challenge, Base64.URL_SAFE))
    .setRpId(response.rpId)
    .setAllowList(response.allowCredentials.map {
        PublicKeyCredentialDescriptor(
            PublicKeyCredentialType.PUBLIC_KEY.toString(),
            Base64.decode(it.id, Base64.URL_SAFE),
            null
        )
    })
    .build()
```

#### Web
```javascript
const options = {
    publicKey: {
        challenge: base64URLToBuffer(response.challenge),
        rpId: response.rpId,
        allowCredentials: response.allowCredentials.map(cred => ({
            type: "public-key",
            id: base64URLToBuffer(cred.id)
        })),
        userVerification: "preferred",
        timeout: 60000
    }
};

const assertion = await navigator.credentials.get(options);
```

### 2.3 인증 완료 요청 - 서버에 보낼 값 생성

서명 후 플랫폼 API가 반환하는 값들을 서버에 전송합니다.

#### 플랫폼별 반환값과 서버 전송 매핑

| 서버 필드 | iOS | Android | Web |
|-----------|-----|---------|-----|
| `id` | `credential.credentialID` | `response.keyHandle` | `credential.id` |
| `rawId` | `credential.credentialID` | `response.keyHandle` | `credential.rawId` |
| `response.clientDataJSON` | `credential.rawClientDataJSON` | `response.clientDataJSON` | `response.clientDataJSON` |
| `response.authenticatorData` | `credential.rawAuthenticatorData` | `response.authenticatorData` | `response.authenticatorData` |
| `response.signature` | `credential.signature` | `response.signature` | `response.signature` |
| `response.userHandle` | `credential.userID` | `response.userHandle` | `response.userHandle` |

#### 서버에 보내는 요청 형식
```json
POST /api/passkey/authenticate/finish
{
  "id": "<credentialID를 Base64URL 인코딩>",
  "rawId": "<credentialID를 Base64URL 인코딩>",
  "type": "public-key",
  "response": {
    "clientDataJSON": "<clientDataJSON 바이트를 Base64URL 인코딩>",
    "authenticatorData": "<authenticatorData 바이트를 Base64URL 인코딩>",
    "signature": "<signature 바이트를 Base64URL 인코딩>",
    "userHandle": "<userHandle 바이트를 Base64URL 인코딩>"
  }
}
```

#### clientDataJSON 구조 (인증 시)
```json
{
  "type": "webauthn.get",
  "challenge": "YXV0aC1jaGFsbGVuZ2U...",
  "origin": "https://your-domain.com",
  "crossOrigin": false
}
```

#### authenticatorData 구조 (바이너리)
| 바이트 | 설명 |
|--------|------|
| 0-31 | rpIdHash (SHA-256 해시) |
| 32 | flags (UP, UV, AT, ED 비트) |
| 33-36 | signCount (4바이트 빅엔디안) |

#### signature
- 서버가 등록 시 저장한 공개키로 검증
- ECDSA 또는 RSA 알고리즘 사용

---

## 3. Base64URL 인코딩/디코딩 헬퍼

모든 바이너리 데이터는 **Base64URL (패딩 없음)** 형식으로 인코딩해야 합니다.

### Swift
```swift
extension Data {
    init?(base64URLDecoded string: String) {
        var base64 = string
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
        while base64.count % 4 != 0 {
            base64.append("=")
        }
        self.init(base64Encoded: base64)
    }

    func base64URLEncodedString() -> String {
        return self.base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }
}
```

### Kotlin
```kotlin
fun String.base64URLDecode(): ByteArray {
    return Base64.decode(this, Base64.URL_SAFE or Base64.NO_PADDING)
}

fun ByteArray.base64URLEncode(): String {
    return Base64.encodeToString(this, Base64.URL_SAFE or Base64.NO_PADDING)
}
```

### JavaScript
```javascript
function base64URLToBuffer(base64url) {
    const base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');
    const padding = '='.repeat((4 - base64.length % 4) % 4);
    const binary = atob(base64 + padding);
    return Uint8Array.from(binary, c => c.charCodeAt(0));
}

function bufferToBase64URL(buffer) {
    const bytes = new Uint8Array(buffer);
    let binary = '';
    bytes.forEach(b => binary += String.fromCharCode(b));
    return btoa(binary)
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/, '');
}
```

---

## 4. 체크리스트

### 등록 시 서버에 보내야 할 값
- [ ] `username`: 사용자 식별자 (등록 시작 요청과 동일)
- [ ] `id`: credentialID (Base64URL)
- [ ] `rawId`: credentialID (Base64URL) - id와 동일
- [ ] `type`: "public-key" (고정값)
- [ ] `response.clientDataJSON`: (Base64URL)
- [ ] `response.attestationObject`: (Base64URL)

### 인증 시 서버에 보내야 할 값
- [ ] `id`: credentialID (Base64URL)
- [ ] `rawId`: credentialID (Base64URL)
- [ ] `type`: "public-key" (고정값)
- [ ] `response.clientDataJSON`: (Base64URL)
- [ ] `response.authenticatorData`: (Base64URL)
- [ ] `response.signature`: (Base64URL)
- [ ] `response.userHandle`: (Base64URL)

---

## 5. 자주 발생하는 오류

| 오류 | 원인 | 해결방법 |
|------|------|----------|
| Challenge not found or expired | challenge 유효시간(60초) 초과 | 등록/인증 시작부터 다시 진행 |
| Invalid origin | origin이 서버 설정과 불일치 | rp.id와 클라이언트 도메인 확인 |
| User not found | 등록되지 않은 사용자 | 등록 먼저 진행 |
| Signature verification failed | 서명 검증 실패 | clientDataJSON, authenticatorData 인코딩 확인 |
| Invalid attestation | attestationObject 파싱 실패 | Base64URL 인코딩 확인 |

---

## 6. 디버깅 팁

1. **요청 전 데이터 확인**: 서버에 보내기 전 각 필드가 올바르게 Base64URL 인코딩되었는지 확인
2. **clientDataJSON 디코딩**: Base64URL 디코딩 후 JSON 파싱하여 challenge, origin 값 확인
3. **네트워크 로깅**: 실제 전송되는 JSON 확인
4. **서버 로그 확인**: 서버에서 어느 단계에서 실패하는지 확인