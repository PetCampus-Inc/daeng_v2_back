package com.petcampus.knockdog.domain.auth.adapter.inbound.web

import com.petcampus.knockdog.domain.auth.application.port.input.LoginCommand
import com.petcampus.knockdog.domain.auth.application.port.input.LoginUseCase
import com.petcampus.knockdog.domain.auth.application.port.input.LogoutCommand
import com.petcampus.knockdog.domain.auth.application.port.input.LogoutUseCase
import com.petcampus.knockdog.domain.auth.application.port.input.RefreshTokenCommand
import com.petcampus.knockdog.domain.auth.application.port.input.RefreshTokenUseCase
import com.petcampus.knockdog.global.response.Response
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 로그인/재발급/로그아웃은 "세션"이라는 실제 리소스(조회 가능한 데이터)가 없어서
 * 억지로 REST 리소스로 묶지 않고 액션 기반 경로로 둔다 — 실무에서 흔히 쓰는 방식.
 * 리프레시 토큰 Redis 레코드를 공유하므로 회전(rotation) 로직 중복을 피하기 위해 한 컨트롤러로 묶는다.
 *
 * AccessToken은 레거시와 동일하게 `Authorization` 응답 헤더로 내려준다 — 프론트가 헤더에서만 읽는다.
 */
@RestController
/**
 * 로그인·재발급·로그아웃은 `v0` 경로 그대로다. 경로도 응답도 레거시와 같아
 * 재설계할 것이 없고, `v1` 트윈을 만들면 프론트가 옮길 동기가 없어 `v0`가 죽지 않는다
 * (docs/rules/api-migration.md §2).
 */
@RequestMapping("/api/v0/auth")
class AuthController(
    private val loginUseCase: LoginUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val authCookieFactory: AuthCookieFactory,
) {
    @PostMapping("/login")
    fun login(
        @CookieValue(AuthCookieFactory.OIDC_AUTH_COOKIE) oidcToken: String,
    ): ResponseEntity<Response<AuthUserResponse>> {
        val result = loginUseCase.login(LoginCommand(oidcToken))
        val refreshCookie = authCookieFactory.refreshTokenCookie(result.tokenPair.refreshToken)
        val expiredOidcCookie = authCookieFactory.expiredOidcAuthCookie()

        return ResponseEntity
            .ok()
            .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + result.tokenPair.accessToken)
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .header(HttpHeaders.SET_COOKIE, expiredOidcCookie.toString())
            .body(Response.success(AuthUserResponse.from(result.user)))
    }

    @PostMapping("/refresh")
    fun refresh(
        @CookieValue(AuthCookieFactory.REFRESH_TOKEN_COOKIE) refreshToken: String,
    ): ResponseEntity<Response<Unit>> {
        val tokenPair = refreshTokenUseCase.refresh(RefreshTokenCommand(refreshToken))
        val refreshCookie = authCookieFactory.refreshTokenCookie(tokenPair.refreshToken)

        return ResponseEntity
            .ok()
            .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + tokenPair.accessToken)
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .body(Response.success<Unit>())
    }

    /**
     * 레거시는 body의 `pushDeviceId`로 해당 기기의 푸시 수신을 끈다. 계약 유지를 위해 받기는 하지만
     * **비활성화는 아직 구현하지 않았다** — notification 도메인이 미이관이다(A-5 탈퇴와 같은 의존).
     */
    @PostMapping("/logout")
    fun logout(
        @CookieValue(AuthCookieFactory.REFRESH_TOKEN_COOKIE, required = false) refreshToken: String?,
        @RequestBody(required = false) request: LogoutRequest? = null,
    ): ResponseEntity<Response<Unit>> {
        logoutUseCase.logout(LogoutCommand(refreshToken ?: ""))

        return ResponseEntity
            .ok()
            .header(HttpHeaders.SET_COOKIE, authCookieFactory.expiredRefreshTokenCookie().toString())
            .body(Response.success<Unit>())
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}

data class LogoutRequest(
    val pushDeviceId: String? = null,
)
