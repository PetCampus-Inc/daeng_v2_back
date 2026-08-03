> 생성: 2026-07-27 17:51 · 최종 수정: 2026-08-03

# auth 도메인 마이그레이션 지시서

- 설계 근거: [`0001`](../adr/0001-legacy-v1-v2-폐기.md) 레거시 폐기, [`0002`](../adr/0002-db-스키마-유지.md)(→[`0010`](../adr/0010-신규-db-인스턴스-스키마-재작성.md)으로 대체됨) 스키마, [`0003`](../adr/0003-헥사고날-정석형-통일.md) 헥사고날 정석형, [`0004`](../adr/0004-api-v0-유지-v1-신규.md) API v0/v1 + 작업 단위 분해, [`0006`](../adr/0006-소셜로그인-id-token-직접검증-유지.md) 소셜 로그인 ID Token 유지. 전체 목록은 `docs/adr/` 폴더 참고(파일명이 일련번호순으로 정렬됨)
- 원본: `knockdog_server` 의 `auth/` 패키지 (컨트롤러 2개, 서비스 5개, 총 58파일)
- 슬라이스 규칙: [`docs/architecture/hexagonal.md`](../architecture/hexagonal.md) (정석형 — `domain/` 순수 모델 + JPA 엔티티 분리 + 매퍼, ArchUnit 경계 강제). 실제 코드 예시는 `domain/owner/`, `domain/auth/`
- **A-0~A-3 구현 완료** (KD3-258, [`docs/plans/KD3-258-user-social-auth.md`](../plans/KD3-258-user-social-auth.md) 참고). 아래 §0 테이블은 KD3-258에서 확정된 신규 스키마 기준으로 갱신됨 — 더 이상 레거시 스키마 참고 자료가 아님. `withdraw_reason`(A-5), `email_verification`(A-6)은 아직 미구현.

이 도메인이 가장 먼저 마이그레이션되는 이유: 다른 모든 도메인의 인증 전제가 되고(`@AuthenticationPrincipal String userId`), owner/pet/memo 등 대부분 도메인이 이 위에서 동작한다.

## 0. 담당 데이터

| 저장소 | 이름 | 비고 |
|---|---|---|
| MySQL (JPA) | `users` | PK는 auto-increment `Long`(내부용), 대외 식별자는 `user_code`(8자 영숫자). `status` 컬럼 없음 — 탈퇴 여부는 `deleted_at`으로 표현([`jpa-entity.md`](../conventions/jpa-entity.md) §2) |
| MySQL (JPA) | `user_addresses` | FK 제약 없이 `user_id` 컬럼만 저장([`jpa-entity.md`](../conventions/jpa-entity.md) §3). HOME 주소가 최소 1개 있어야 함(도메인 불변식, `User.create`) |
| MySQL (JPA) | `social_users` | `(provider, provider_id)` 유니크. **`email`은 유니크 아님** — 동일 이메일 다른 provider row가 공존할 수 있다(레거시 `VerifyOidcService` 로직 그대로 포팅, PENDING 상태). `user_id`는 nullable(연동 전 상태 존재), FK 제약 없음 |
| Redis (`@RedisHash`) | `refresh_token` | TTL 30일. kindergarten 도메인과는 별개의 신규 Redis 인스턴스 사용(공유 안 함) |
| MySQL (JPA) | `withdraw_reason` | **미구현** (A-5, 후속 티켓) |
| Redis (`@RedisHash`) | `email_verification` | **미구현** (A-6, 후속 티켓) — 새 스키마는 비밀번호 없는 소셜 전용 가입이라 이메일 인증의 역할 자체가 레거시와 다를 수 있음, 착수 전 재확인 필요 |

## 1. 마이그레이션 대상 엔드포인트 (14 → 12)

| 기존 (`v0`, 그대로 유지) | 신규 (`v1`) | 판정 |
|---|---|---|
| `POST /api/v0/auth/verify/oidc` | `POST /api/v1/auth/oidc-verifications` | **구현 완료** (A-1) |
| `POST /api/v0/auth/login` | `POST /api/v1/auth/sessions` | **구현 완료** (A-2) |
| `POST /api/v0/auth/logout` | `DELETE /api/v1/auth/sessions` | **구현 완료** (A-2) |
| `POST /api/v0/auth/refresh` | `PATCH /api/v1/auth/sessions` | **구현 완료** (A-2) |
| `POST /api/v0/auth/email/send` | `POST /api/v1/email-verifications` | 이관 (미착수) |
| `POST /api/v0/auth/email/verify` | `PATCH /api/v1/email-verifications` | 이관 (미착수) |
| `GET /api/v0/auth/email/verification` | `GET /api/v1/email-verifications` | 이관 (미착수) |
| `GET /api/v0/auth/dev/{id}` | — (`v1` 미제공) | 이관, 단 **`@Profile("local")` 가드 신규 추가 필수** (§3, 현재 운영 노출) — 미착수 |
| `GET /api/v0/user/social/user` | `GET /api/v1/users/me/social-account` | 이관 (미착수, A-4) |
| `POST /api/v0/user/social/reconnect` | `PUT /api/v1/users/me/social-account` | 이관 (미착수, A-4) |
| `POST /api/v0/user/register` | `POST /api/v1/users` | **구현 완료** (A-3) |
| `POST /api/v0/user/withdraw` | `DELETE /api/v1/users/me` | 이관 (미착수, A-5) |

**삭제 (프론트 호출 0건, 메인 설계 문서 §4.1 근거):**
- `POST /api/v0/user/social/delete` — 미사용
- `POST /api/v0/user/restore` — 미사용. 참고로 이 엔드포인트는 인증 없이 임의의 `id`(Long PK)만으로 탈퇴 계정을 복구시킬 수 있어 미사용이 아니었어도 재검토 대상이었음

## 2. 작업 단위 분해 (선행 기반 + 유스케이스 슬라이스)

메인 설계 문서 §6의 kindergarten 패턴을 그대로 적용한다. **A-0가 끝나야 A-1~A-7이 병렬로 열린다.**

### A-0 · 기반 (선행, 병렬 불가) — **구현 완료**

```
domain/auth/
  domain/                        User, SocialUser, WithdrawReason, UserAddress 순수 모델 + VO
  adapter/outbound/persistence/  JPA 엔티티(User/SocialUser/WithdrawReason/UserAddress) + Repository + Mapper
  adapter/outbound/cache/        RedisRefreshToken, RedisEmailVerification 어댑터 (RedisHash 그대로)
  adapter/outbound/oidc/         AppleTokenVerifier, GoogleTokenVerifier, KakaoTokenVerifier
                                 (원본 lib/oidc/verifier/* 로직 그대로 이관, 로직 변경 없음)
  application/port/output/       TokenPort(JWT 발급/검증), OidcVerifierPort
```

- **가져오지 않는 것(D9):** `config/OAuth2SuccessHandler`, `CustomOAuth2User`, `OAuth2UserCustomService`, `OAuth2AuthorizationRequestBasedOnCookieRepository`, `spring-boot-starter-oauth2-client` 의존성. Spring Security `.oauth2Login()` 흐름을 위한 코드인데 `.oauth2Login()` 호출도 클라이언트 등록도 없어 완전히 죽어 있고, 프론트/앱이 이미 ID Token 직접 검증 방식을 쓰고 있어 애초에 이 흐름으로 바꿀 이유도 없다.

- `JwtProvider`(원본 `auth/lib/jwt/JwtProvider.java`) 역할은 `TokenPort` 구현체로 옮긴다. `application` 계층은 이 포트에만 의존하고 `io.jsonwebtoken.*`을 직접 참조하지 않는다(ArchUnit 규칙 2 적용 대상 확장 검토).
- 액세스 토큰(헤더) / 리프레시 토큰(HttpOnly+Secure+SameSite=None 쿠키) / OIDC 임시 토큰(쿠키, 20분) 이라는 **3종 토큰 체계**는 원본 그대로 유지한다. 쿠키 속성을 하나라도 바꾸면 프론트가 깨진다.
- JWT 시크릿/발급자/만료시간 등 설정값(`application.yml`의 `jwt.*`)은 그대로 가져온다.

### A-1 소셜 인증(OIDC 검증) — **구현 완료**
- `VerifyOidcUseCase` — 1개 엔드포인트
- provider+providerId로 기존 연동 조회 → LINKED/UNLINKED/PENDING 3분기 → OIDC 임시 토큰 발급
- 동일 이메일 다른 provider 존재 시 `PENDING`/`EMAIL_ALREADY_EXISTS` 처리 로직 그대로 이관

### A-2 로그인/토큰 발급/재발급/로그아웃 — **구현 완료**
- `LoginUseCase`, `RefreshTokenUseCase`, `LogoutUseCase` — 3개 엔드포인트
- 이 셋은 리프레시 토큰 Redis 레코드를 공유하므로 한 슬라이스로 묶는다(따로 쪼개면 토큰 회전 로직이 두 곳에 중복됨)
- 탈퇴 유저 로그인 시도 시 "탈퇴 후 7일 이내 재가입 제한" 분기(`REJOINING_RESTRICTION_PERIOD`) 포함

### A-3 회원가입 — **구현 완료**
- `RegisterUseCase` — 1개 엔드포인트
- 주소 목록 중 `HOME` 타입이 최소 1개 있어야 하는 불변식은 **순수 도메인 모델(`User.create`)에 유지** — JPA/서비스 레이어가 아니라 도메인이 검증

### A-4 소셜 계정 연동 조회/재연동
- `GetLinkedSocialUserUseCase`, `ReconnectSocialUserUseCase` — 2개 엔드포인트

### A-5 회원 탈퇴
- `WithdrawUserUseCase` — 1개 엔드포인트
- **크로스 도메인 의존 주의**: 탈퇴 가능 여부 검증이 `owner` 도메인의 "활성 원장 권한 보유 여부"를 확인한다(원본: `UserSchoolRoleRepository.findFirstBy...`). 정석형 헥사고날에서는 auth의 `application` 레이어가 owner 도메인의 JPA 리포지토리를 직접 참조하면 안 된다 — **owner 쪽에 조회 포트(예: `HasActiveOwnerRolePort`)를 만들어 auth가 그 포트에만 의존**하도록 한다. owner 도메인 슬라이스 작업과 순서를 맞출 것.
- 탈퇴 후 7일 뒤 배치 삭제(`WithdrawnUserCleanupScheduler`)는 다른 도메인 데이터까지 함께 지우는 크로스 도메인 배치이므로 **이 슬라이스 범위에서 제외**하고 별도 태스크로 분리한다.

### A-6 이메일 인증
- `SendVerificationEmailUseCase`, `VerifyEmailUseCase`, `CheckEmailVerificationUseCase` — 3개 엔드포인트
- Thymeleaf 템플릿(`templates/email-verification.html`) + 인라인 이미지 첨부까지 그대로 이관
- 시도 횟수 제한(15회/30분), 인증 만료(3분) 값 그대로 유지

### A-7 개발용 로그인
- `DevLoginUseCase` — 1개 엔드포인트
- **`@Profile("local")` 또는 이에 준하는 가드를 반드시 추가.** 원본은 운영 프로필에서도 열려 있어 유저 PK만 알면 그 사람 토큰을 발급받을 수 있었다(메인 설계 문서 §3). 프론트가 `DEV_LOGIN_ID`로 실사용 중이므로 기능 자체는 유지하되 노출 프로필만 제한한다.

## 3. 인가 정책 (이 도메인에 한정된 노트)

- 원본의 `TokenAuthenticationFilter`는 `@PrivateAccess` 붙은 엔드포인트만 인증을 요구하고, 나머지는 토큰이 있으면 검증하되 없어도 통과시키는 "선택적 인증"이다. 신규 서버는 메인 설계 문서 §8(기본 deny)을 따르되, **A-1~A-4, A-6, A-7은 로그인 전 단계이므로 명시적으로 공개(permit) 목록에 올려야 한다.** 인증이 필요한 것은 `withdraw`(A-5) 뿐이다.
  - **구현 완료(KD3-258)**: `global/config/SecurityConfig.kt`가 `/api/v1/auth/oidc-verifications`, `/api/v1/auth/sessions`, `/api/v1/users`를 permit 목록에 올리고 나머지는 `authenticated()`. 커스텀 필터(`domain/auth/adapter/inbound/security/AccessTokenAuthenticationFilter`)는 액세스 토큰이 있으면 `SecurityContext`에 `ROLE_USER`를 채우고, 없거나 유효하지 않으면 그대로 통과시켜 `authorizeHttpRequests`가 최종 판정하게 한다. 이 기본 deny 도입으로 기존 `owner` 도메인의 `/owners` 엔드포인트도 인증을 요구하게 되는 부수효과가 있음 — `owner` 도메인 쪽 대응은 별도 판단 필요.
- `application.yml`의 `security.roles.*`(`ROLE_MEMBER`, `ROLE_OWNER` 등)는 **v3에서 사용되지 않는 죽은 설정**이다(`SecurityRoleProperties` 참조처가 정의 클래스 자신뿐). `TokenAuthenticationFilter`는 인증된 모든 사용자에게 단일 권한(`ROLE_USER`)만 부여한다. 세분화된 역할(원장 권한 등)은 Spring Security 권한이 아니라 `owner` 도메인이 자체 테이블로 검증하는 방식이며, auth 도메인은 이 패턴을 유지한다 — 신규 서버에서 역할 기반 Spring Security를 새로 도입하지 않는다.

## 4. 참조

- 원본 컨트롤러: `auth/controller/UserAuthController.java`, `auth/controller/UserController.java`
- 원본 서비스: `auth/service/{VerifyOidcService,UserAuthService,UserService,TokenService,EmailVerifyService}.java`
- 원본 OIDC 검증기: `auth/lib/oidc/verifier/{Apple,Google,Kakao}TokenVerifier.java` (로직 변경 없이 그대로 포팅)
- 골든 테스트 대상(메인 설계 문서 §9): A-1~A-4, A-6, A-7은 `v0`↔`v1` 응답 동등성 비교, A-5는 탈퇴 후 상태 전이 비교
