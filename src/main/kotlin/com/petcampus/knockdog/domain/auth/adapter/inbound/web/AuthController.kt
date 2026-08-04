package com.petcampus.knockdog.domain.auth.adapter.inbound.web

import com.petcampus.knockdog.domain.auth.application.port.input.LoginCommand
import com.petcampus.knockdog.domain.auth.application.port.input.LoginUseCase
import com.petcampus.knockdog.domain.auth.application.port.input.LogoutCommand
import com.petcampus.knockdog.domain.auth.application.port.input.LogoutUseCase
import com.petcampus.knockdog.domain.auth.application.port.input.RefreshTokenCommand
import com.petcampus.knockdog.domain.auth.application.port.input.RefreshTokenUseCase
import com.petcampus.knockdog.domain.auth.application.port.input.TokenPair
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 로그인/재발급/로그아웃은 "세션"이라는 실제 리소스(조회 가능한 데이터)가 없어서
 * 억지로 REST 리소스로 묶지 않고 액션 기반 경로로 둔다 — 실무에서 흔히 쓰는 방식.
 * 리프레시 토큰 Redis 레코드를 공유하므로 회전(rotation) 로직 중복을 피하기 위해 한 컨트롤러로 묶는다.
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val loginUseCase: LoginUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val authCookieFactory: AuthCookieFactory,
) {
    @PostMapping("/login")
    fun login(
        @CookieValue(AuthCookieFactory.OIDC_AUTH_COOKIE) oidcToken: String,
    ): ResponseEntity<TokenResponse> {
        val tokenPair = loginUseCase.login(LoginCommand(oidcToken))
        return tokenResponse(tokenPair)
    }

    @PostMapping("/refresh")
    fun refresh(
        @CookieValue(AuthCookieFactory.REFRESH_TOKEN_COOKIE) refreshToken: String,
    ): ResponseEntity<TokenResponse> {
        val tokenPair = refreshTokenUseCase.refresh(RefreshTokenCommand(refreshToken))
        return tokenResponse(tokenPair)
    }

    @PostMapping("/logout")
    fun logout(
        @CookieValue(AuthCookieFactory.REFRESH_TOKEN_COOKIE, required = false) refreshToken: String?,
    ): ResponseEntity<Void> {
        logoutUseCase.logout(LogoutCommand(refreshToken ?: ""))

        return ResponseEntity
            .noContent()
            .header(HttpHeaders.SET_COOKIE, authCookieFactory.expiredRefreshTokenCookie().toString())
            .build()
    }

    private fun tokenResponse(tokenPair: TokenPair): ResponseEntity<TokenResponse> {
        val refreshCookie = authCookieFactory.refreshTokenCookie(tokenPair.refreshToken)
        val expiredOidcCookie = authCookieFactory.expiredOidcAuthCookie()

        return ResponseEntity
            .ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .header(HttpHeaders.SET_COOKIE, expiredOidcCookie.toString())
            .body(TokenResponse(tokenPair.accessToken))
    }
}

data class TokenResponse(
    val accessToken: String,
)
