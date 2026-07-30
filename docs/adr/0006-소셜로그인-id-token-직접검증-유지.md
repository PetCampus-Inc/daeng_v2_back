> 생성: 2026-07-26 · 최종 수정: 2026-07-28 16:07

# 0006. 소셜 로그인은 ID Token 직접 검증 방식을 그대로 이관

## 맥락

현재 서버는 `SecurityFilterChain` 빈이 하나뿐이고(`WebOAuthSecurityConfig`), `AuthenticationManager`/`UserDetailsService`/`.oauth2Login()` 없이 커스텀 `OncePerRequestFilter`(`TokenAuthenticationFilter`)가 JWT를 직접 검증해 `SecurityContextHolder`에 수동으로 꽂는 구조다. 실제 신원 확인은 OIDC ID Token 검증(Apple/Google/Kakao public key)과 자체 발급 JWT(jjwt)로 전부 처리된다. 신규 서버에서 Spring Security의 정석적인 `.oauth2Login()`(인가 코드/리다이렉트) 흐름으로 바꿀지 결정이 필요했다.

## 검토한 후보

1. Spring Security `.oauth2Login()`(인가 코드/리다이렉트 방식)으로 전환한다.
2. 지금처럼 클라이언트가 OIDC ID Token을 직접 받아 서버에 JSON으로 전송하고, 서버가 이를 검증하는 방식을 그대로 이관한다.

## 결정

**후보 2 — ID Token 직접 검증 방식 그대로 이관.**

근거: `.oauth2Login()`은 브라우저 리다이렉트를 전제로 하는데, 프론트(`daeng_v2_front`)와 모바일 앱은 카카오/구글 SDK로 클라이언트에서 ID Token을 직접 받아 JSON으로 전송하는 구조(`entities/social-user/api/verifyOidc.ts`, 앱의 `@react-native-kakao/user` 등 네이티브 SDK 의존)라 서로 호환되지 않는다.

`.oauth2Login()`용으로 존재하던 `config/OAuth2SuccessHandler`, `CustomOAuth2User`, `OAuth2UserCustomService`, `OAuth2AuthorizationRequestBasedOnCookieRepository` 4개 클래스와 `spring-boot-starter-oauth2-client` 의존성은 `.oauth2Login()` 호출도, `application.yml`의 클라이언트 등록도 없어 이미 완전히 죽은 코드로 확인됐다 — 신규 서버로 가져가지 않고 삭제한다.

## 결과

- 신규 서버도 커스텀 `OncePerRequestFilter`가 JWT를 직접 검증하는 구조를 유지한다. Spring Security는 `authorizeHttpRequests`(경로별 인증 요구 판정)와 `SecurityContext` 저장소로만 쓴다.
- `OAuth2SuccessHandler` 등 죽은 코드 4개 클래스 + `spring-boot-starter-oauth2-client` 의존성은 이관하지 않는다.
- 인가(경로별 접근 제어) 정책 자체의 전환은 [`0007-인가-기본-deny-전환.md`](0007-인가-기본-deny-전환.md) 참고.
