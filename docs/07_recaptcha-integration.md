# reCAPTCHA v2/v3 통합 가이드

## 개요

이 프로젝트는 Google reCAPTCHA v3와 v2를 조합하여 봇 방지 기능을 구현합니다.

- **v3**: 사용자 행동 기반 점수 측정 (invisible)
- **v2**: 점수가 낮을 경우 체크박스 방식으로 폴백

## 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client (Browser)                         │
├─────────────────────────────────────────────────────────────────┤
│  1. grecaptcha.execute() → v3 토큰 획득                          │
│  2. HTTP Header로 토큰 전송                                       │
│     - X-Captcha-Token: v3 토큰                                   │
│     - X-Captcha-Token-V2: v2 토큰 (필요시)                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Server (Spring)                          │
├─────────────────────────────────────────────────────────────────┤
│  CaptchaAspect (@RequireCaptcha 어노테이션 처리)                   │
│       │                                                          │
│       ▼                                                          │
│  RecaptchaService (비즈니스 로직)                                  │
│       │                                                          │
│       ▼                                                          │
│  RecaptchaClient (Google API 호출)                                │
│       │                                                          │
│       ▼                                                          │
│  Google reCAPTCHA API (https://www.google.com/recaptcha/api)     │
└─────────────────────────────────────────────────────────────────┘
```

## 검증 흐름

```
┌──────────┐     v3 토큰      ┌──────────┐     검증 요청     ┌──────────┐
│  Client  │ ──────────────▶ │  Server  │ ──────────────▶ │  Google  │
└──────────┘                 └──────────┘                 └──────────┘
                                   │
                                   ▼
                          ┌───────────────┐
                          │ score >= 0.5? │
                          └───────────────┘
                           │           │
                         Yes          No
                           │           │
                           ▼           ▼
                        [PASS]    [REQUIRE_V2]
                                       │
                                       ▼
                          ┌─────────────────────┐
                          │ HTTP 428 응답        │
                          │ {requireV2Captcha}  │
                          └─────────────────────┘
                                       │
                                       ▼
                          ┌─────────────────────┐
                          │ Client: v2 모달 표시 │
                          │ 체크박스 완료 후 재요청│
                          └─────────────────────┘
```

## 서버 구성 요소

### 1. 설정 (application.yml)

```yaml
recaptcha:
  enabled: ${RECAPTCHA_ENABLED:true}
  verify-url: https://www.google.com/recaptcha/api/siteverify
  v3:
    site-key: ${RECAPTCHA_V3_SITE_KEY:}
    secret-key: ${RECAPTCHA_V3_SECRET_KEY:}
    threshold: 0.5  # 0.0 ~ 1.0 (높을수록 엄격)
  v2:
    site-key: ${RECAPTCHA_V2_SITE_KEY:}
    secret-key: ${RECAPTCHA_V2_SECRET_KEY:}
```

### 2. 파일 구조

```
server/src/main/java/com/example/passkey/global/
├── captcha/
│   ├── RequireCaptcha.java        # AOP 어노테이션
│   ├── CaptchaAspect.java         # AOP 로직 (헤더에서 토큰 추출)
│   ├── RecaptchaService.java      # 검증 비즈니스 로직
│   ├── RecaptchaClient.java       # Google API 호출
│   ├── RecaptchaResponse.java     # Google API 응답 DTO
│   ├── CaptchaException.java      # 검증 실패 예외
│   └── RequireV2CaptchaException.java  # v2 필요 예외
├── config/
│   └── RecaptchaConfig.java       # 설정 + WebClient Bean
└── exception/
    └── GlobalExceptionHandler.java # 예외 처리
```

### 3. 주요 클래스

#### @RequireCaptcha 어노테이션

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireCaptcha {
}
```

컨트롤러 메서드에 적용:

```java
@PostMapping("/register/start")
@RequireCaptcha
public ResponseEntity<RegistrationStartResponse> startRegistration(...) {
    // reCAPTCHA 검증 후 실행됨
}
```

#### CaptchaAspect

HTTP 헤더에서 토큰을 추출하여 검증:

| 헤더 | 용도 |
|------|------|
| `X-Captcha-Token` | reCAPTCHA v3 토큰 |
| `X-Captcha-Token-V2` | reCAPTCHA v2 토큰 |

#### RecaptchaService.CaptchaResult

| 결과 | 설명 |
|------|------|
| `PASS` | 검증 성공 |
| `REQUIRE_V2` | v3 점수가 낮아 v2 필요 |
| `FAIL` | 검증 실패 (예외 발생) |

### 4. HTTP 응답 코드

| 상태 코드 | 의미 | 응답 본문 |
|-----------|------|-----------|
| 200 | 검증 성공 | 정상 응답 |
| 428 | v2 필요 | `{"requireV2Captcha": true}` |
| 403 | 검증 실패 | `{"error": "CAPTCHA_FAILED", "message": "..."}` |

## 클라이언트 구현

### 1. reCAPTCHA 스크립트 로드

```html
<!-- v3: render 파라미터에 site key 지정 -->
<script src="https://www.google.com/recaptcha/api.js?render=YOUR_V3_SITE_KEY" async defer></script>
```

### 2. v3 토큰 획득

```javascript
const RECAPTCHA_V3_SITE_KEY = 'YOUR_V3_SITE_KEY';

async function getRecaptchaV3Token(action) {
    return new Promise((resolve, reject) => {
        grecaptcha.ready(() => {
            grecaptcha.execute(RECAPTCHA_V3_SITE_KEY, { action })
                .then(resolve)
                .catch(reject);
        });
    });
}
```

### 3. API 요청 시 헤더에 토큰 포함

```javascript
async function register(v2Token = null) {
    const headers = { 'Content-Type': 'application/json' };

    if (v2Token) {
        headers['X-Captcha-Token-V2'] = v2Token;
    } else {
        const captchaToken = await getRecaptchaV3Token('register');
        if (captchaToken) {
            headers['X-Captcha-Token'] = captchaToken;
        }
    }

    const response = await fetch('/api/auth/register/start', {
        method: 'POST',
        headers,
        body: JSON.stringify({ username, displayName })
    });

    // HTTP 428: v2 필요
    if (response.status === 428) {
        const data = await response.json();
        if (data.requireV2Captcha) {
            const v2Token = await showRecaptchaV2Modal();
            return register(v2Token);  // v2 토큰으로 재시도
        }
    }

    // ... 나머지 처리
}
```

### 4. v2 모달 구현

```javascript
const RECAPTCHA_V2_SITE_KEY = 'YOUR_V2_SITE_KEY';
let recaptchaV2WidgetId = null;

function showRecaptchaV2Modal() {
    return new Promise((resolve, reject) => {
        const modal = document.getElementById('recaptchaModal');
        const container = document.getElementById('recaptchaV2Container');

        modal.classList.add('show');

        window.recaptchaResolve = resolve;
        window.recaptchaReject = reject;

        grecaptcha.ready(() => {
            if (recaptchaV2WidgetId !== null) {
                // 재사용: reset
                grecaptcha.reset(recaptchaV2WidgetId);
            } else {
                // 최초: render
                recaptchaV2WidgetId = grecaptcha.render(container, {
                    sitekey: RECAPTCHA_V2_SITE_KEY,
                    callback: (token) => {
                        window.recaptchaResolve?.(token);
                        window.recaptchaResolve = null;
                        window.recaptchaReject = null;
                        modal.classList.remove('show');
                    },
                    'expired-callback': () => {
                        window.recaptchaReject?.(new Error('expired'));
                        modal.classList.remove('show');
                    }
                });
            }
        });
    });
}
```

## 환경 변수 설정

```bash
# .env 또는 환경 변수
export RECAPTCHA_ENABLED=true
export RECAPTCHA_V3_SITE_KEY=6Lf8...
export RECAPTCHA_V3_SECRET_KEY=6Lf8...
export RECAPTCHA_V2_SITE_KEY=6LcF...
export RECAPTCHA_V2_SECRET_KEY=6LcF...
```

## 개발/테스트 시 비활성화

```yaml
# application.yml 또는 application-dev.yml
recaptcha:
  enabled: false
```

`enabled: false`로 설정하면 `RecaptchaService.verify()`가 항상 `PASS`를 반환합니다.

## Google reCAPTCHA Admin 설정

1. [Google reCAPTCHA Admin](https://www.google.com/recaptcha/admin) 접속
2. v3 사이트 생성 (Score based)
3. v2 사이트 생성 (Checkbox)
4. 도메인 등록 (localhost 포함)
5. Site Key / Secret Key 획득

## 트러블슈팅

### macOS에서 DNS 해결 실패

```
Failed to resolve 'www.google.com'
io.netty.resolver.dns.macos.MacOSDnsServerAddressStreamProvider
```

**해결**: WebClient에 JDK HttpClient 사용

```java
@Bean
public WebClient recaptchaWebClient() {
    HttpClient httpClient = HttpClient.newHttpClient();
    return WebClient.builder()
            .clientConnector(new JdkClientHttpConnector(httpClient))
            .baseUrl(verifyUrl)
            .build();
}
```

### v2 체크박스 재표시 안됨

v2 위젯을 여러 번 사용할 때 `grecaptcha.reset(widgetId)` 호출 필요:

```javascript
if (recaptchaV2WidgetId !== null) {
    grecaptcha.reset(recaptchaV2WidgetId);
} else {
    recaptchaV2WidgetId = grecaptcha.render(...);
}
```

### 토큰이 헤더에 안들어감

reCAPTCHA 스크립트 로드 방식 확인:

```html
<!-- 올바른 방식 (v3) -->
<script src="https://www.google.com/recaptcha/api.js?render=YOUR_SITE_KEY"></script>

<!-- 잘못된 방식 -->
<script src="https://www.google.com/recaptcha/api.js?render=explicit"></script>
```

`grecaptcha.ready()` 사용하여 로드 완료 대기:

```javascript
function waitForRecaptcha() {
    return new Promise((resolve) => {
        if (typeof grecaptcha !== 'undefined' && grecaptcha.ready) {
            grecaptcha.ready(resolve);
        } else {
            const check = setInterval(() => {
                if (typeof grecaptcha !== 'undefined' && grecaptcha.ready) {
                    clearInterval(check);
                    grecaptcha.ready(resolve);
                }
            }, 100);
        }
    });
}
```
