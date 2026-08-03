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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth/sessions")
class AuthSessionController(
    private val loginUseCase: LoginUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val authCookieFactory: AuthCookieFactory,
) {
    @PostMapping
    fun login(
        @CookieValue(AuthCookieFactory.OIDC_AUTH_COOKIE) oidcToken: String,
    ): ResponseEntity<TokenResponse> {
        val tokenPair = loginUseCase.login(LoginCommand(oidcToken))
        return tokenResponse(tokenPair)
    }

    @PatchMapping
    fun refresh(
        @CookieValue(AuthCookieFactory.REFRESH_TOKEN_COOKIE) refreshToken: String,
    ): ResponseEntity<TokenResponse> {
        val tokenPair = refreshTokenUseCase.refresh(RefreshTokenCommand(refreshToken))
        return tokenResponse(tokenPair)
    }

    @DeleteMapping
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
