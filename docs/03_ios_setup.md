# iOS Passkey 클라이언트 가이드

## 요구사항

- iOS 16.0 이상
- Xcode 15 이상
- Apple Developer 계정 (Associated Domains 설정 필요)

## 프로젝트 설정

### 1. Xcode 프로젝트 생성
1. Xcode에서 새 iOS App 프로젝트 생성
2. Interface: SwiftUI 또는 Storyboard
3. Language: Swift

### 2. Capabilities 설정
1. 프로젝트 > Signing & Capabilities
2. **+ Capability** 클릭
3. **Associated Domains** 추가
4. 도메인 추가: `webcredentials:your-domain.com`

### 3. apple-app-site-association 파일
서버의 `.well-known/apple-app-site-association`에 다음 내용 배포:

```json
{
  "webcredentials": {
    "apps": ["TEAM_ID.com.example.passkey-app"]
  }
}
```

---

## Swift 코드 구현

### PasskeyManager.swift

```swift
import AuthenticationServices
import Foundation

class PasskeyManager: NSObject {

    private let domain = "your-domain.com"  // 실제 도메인으로 변경
    private let baseURL = "https://your-domain.com/api/passkey"

    private var authenticationAnchor: ASPresentationAnchor?
    private var registrationContinuation: CheckedContinuation<Void, Error>?
    private var authenticationContinuation: CheckedContinuation<String, Error>?

    // MARK: - Registration (Passkey 등록)

    func register(username: String, displayName: String, anchor: ASPresentationAnchor) async throws {
        self.authenticationAnchor = anchor

        // 1. 서버에서 challenge 받기
        let startResponse = try await startRegistration(username: username, displayName: displayName)

        // 2. Passkey 생성
        let challenge = Data(base64URLEncoded: startResponse.challenge)!
        let userID = Data(base64URLEncoded: startResponse.user.id)!

        let publicKeyCredentialProvider = ASAuthorizationPlatformPublicKeyCredentialProvider(
            relyingPartyIdentifier: domain
        )

        let registrationRequest = publicKeyCredentialProvider.createCredentialRegistrationRequest(
            challenge: challenge,
            name: username,
            userID: userID
        )

        let authController = ASAuthorizationController(authorizationRequests: [registrationRequest])
        authController.delegate = self
        authController.presentationContextProvider = self

        return try await withCheckedThrowingContinuation { continuation in
            self.registrationContinuation = continuation
            authController.performRequests()
        }
    }

    // MARK: - Authentication (Passkey 인증)

    func authenticate(username: String?, anchor: ASPresentationAnchor) async throws -> String {
        self.authenticationAnchor = anchor

        // 1. 서버에서 challenge 받기
        let startResponse = try await startAuthentication(username: username)

        // 2. Passkey로 서명
        let challenge = Data(base64URLEncoded: startResponse.challenge)!

        let publicKeyCredentialProvider = ASAuthorizationPlatformPublicKeyCredentialProvider(
            relyingPartyIdentifier: domain
        )

        let assertionRequest = publicKeyCredentialProvider.createCredentialAssertionRequest(
            challenge: challenge
        )

        let authController = ASAuthorizationController(authorizationRequests: [assertionRequest])
        authController.delegate = self
        authController.presentationContextProvider = self

        return try await withCheckedThrowingContinuation { continuation in
            self.authenticationContinuation = continuation
            authController.performRequests()
        }
    }

    // MARK: - API Calls

    private func startRegistration(username: String, displayName: String) async throws -> RegistrationStartResponse {
        let url = URL(string: "\(baseURL)/register/start")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body = ["username": username, "displayName": displayName]
        request.httpBody = try JSONEncoder().encode(body)

        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(RegistrationStartResponse.self, from: data)
    }

    private func finishRegistration(credential: ASAuthorizationPlatformPublicKeyCredentialRegistration, username: String) async throws {
        let url = URL(string: "\(baseURL)/register/finish")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body: [String: Any] = [
            "username": username,
            "id": credential.credentialID.base64URLEncodedString(),
            "rawId": credential.credentialID.base64URLEncodedString(),
            "type": "public-key",
            "response": [
                "clientDataJSON": credential.rawClientDataJSON.base64URLEncodedString(),
                "attestationObject": credential.rawAttestationObject!.base64URLEncodedString()
            ]
        ]

        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        let (_, _) = try await URLSession.shared.data(for: request)
    }

    private func startAuthentication(username: String?) async throws -> AuthenticationStartResponse {
        let url = URL(string: "\(baseURL)/authenticate/start")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        if let username = username {
            request.httpBody = try JSONEncoder().encode(["username": username])
        } else {
            request.httpBody = "{}".data(using: .utf8)
        }

        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(AuthenticationStartResponse.self, from: data)
    }

    private func finishAuthentication(credential: ASAuthorizationPlatformPublicKeyCredentialAssertion) async throws -> String {
        let url = URL(string: "\(baseURL)/authenticate/finish")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body: [String: Any] = [
            "id": credential.credentialID.base64URLEncodedString(),
            "rawId": credential.credentialID.base64URLEncodedString(),
            "type": "public-key",
            "response": [
                "clientDataJSON": credential.rawClientDataJSON.base64URLEncodedString(),
                "authenticatorData": credential.rawAuthenticatorData.base64URLEncodedString(),
                "signature": credential.signature.base64URLEncodedString(),
                "userHandle": credential.userID.base64URLEncodedString()
            ]
        ]

        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        let (data, _) = try await URLSession.shared.data(for: request)

        let response = try JSONDecoder().decode(AuthenticationFinishResponse.self, from: data)
        return response.username
    }
}

// MARK: - ASAuthorizationControllerDelegate

extension PasskeyManager: ASAuthorizationControllerDelegate {

    func authorizationController(controller: ASAuthorizationController,
                                didCompleteWithAuthorization authorization: ASAuthorization) {
        switch authorization.credential {
        case let credential as ASAuthorizationPlatformPublicKeyCredentialRegistration:
            // 등록 완료
            Task {
                do {
                    try await finishRegistration(credential: credential, username: "stored_username")
                    registrationContinuation?.resume(returning: ())
                } catch {
                    registrationContinuation?.resume(throwing: error)
                }
            }

        case let credential as ASAuthorizationPlatformPublicKeyCredentialAssertion:
            // 인증 완료
            Task {
                do {
                    let username = try await finishAuthentication(credential: credential)
                    authenticationContinuation?.resume(returning: username)
                } catch {
                    authenticationContinuation?.resume(throwing: error)
                }
            }

        default:
            break
        }
    }

    func authorizationController(controller: ASAuthorizationController,
                                didCompleteWithError error: Error) {
        registrationContinuation?.resume(throwing: error)
        authenticationContinuation?.resume(throwing: error)
    }
}

// MARK: - ASAuthorizationControllerPresentationContextProviding

extension PasskeyManager: ASAuthorizationControllerPresentationContextProviding {
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        return authenticationAnchor!
    }
}

// MARK: - Response Models

struct RegistrationStartResponse: Codable {
    let challenge: String
    let user: UserInfo

    struct UserInfo: Codable {
        let id: String
        let name: String
        let displayName: String
    }
}

struct AuthenticationStartResponse: Codable {
    let challenge: String
    let rpId: String
}

struct AuthenticationFinishResponse: Codable {
    let success: Bool
    let username: String
}

// MARK: - Data Extension

extension Data {
    init?(base64URLEncoded string: String) {
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

---

## 사용 예시 (SwiftUI)

```swift
import SwiftUI

struct ContentView: View {
    @State private var username = ""
    @State private var message = ""

    private let passkeyManager = PasskeyManager()

    var body: some View {
        VStack(spacing: 20) {
            TextField("Username", text: $username)
                .textFieldStyle(.roundedBorder)
                .padding()

            Button("Passkey 등록") {
                Task {
                    do {
                        try await passkeyManager.register(
                            username: username,
                            displayName: username,
                            anchor: UIApplication.shared.windows.first!
                        )
                        message = "등록 성공!"
                    } catch {
                        message = "등록 실패: \(error.localizedDescription)"
                    }
                }
            }

            Button("Passkey 로그인") {
                Task {
                    do {
                        let user = try await passkeyManager.authenticate(
                            username: username,
                            anchor: UIApplication.shared.windows.first!
                        )
                        message = "로그인 성공: \(user)"
                    } catch {
                        message = "로그인 실패: \(error.localizedDescription)"
                    }
                }
            }

            Text(message)
                .foregroundColor(.blue)
        }
    }
}
```

---

## 테스트 시 주의사항

1. **시뮬레이터 제한**: Passkey는 실제 기기에서만 동작 (Face ID/Touch ID 필요)
2. **HTTPS 필수**: localhost 제외하고 HTTPS만 지원
3. **Associated Domains**: 도메인 설정이 올바르게 되어야 함
4. **개발 테스트**: ngrok 등을 사용해 로컬 서버를 HTTPS로 노출 가능
