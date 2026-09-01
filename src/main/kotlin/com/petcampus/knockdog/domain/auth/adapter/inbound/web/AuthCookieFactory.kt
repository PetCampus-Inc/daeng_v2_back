package com.petcampus.knockdog.domain.auth.adapter.inbound.web

import com.petcampus.knockdog.domain.auth.adapter.outbound.jwt.JwtProperties
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component

@Component
class AuthCookieFactory(
    private val jwtProperties: JwtProperties,
) {
    fun oidcAuthCookie(token: String): ResponseCookie = build(OIDC_AUTH_COOKIE, token, jwtProperties.token.durations.oidcAuth / 1000)

    fun expiredOidcAuthCookie(): ResponseCookie = build(OIDC_AUTH_COOKIE, "", 0)

    fun refreshTokenCookie(token: String): ResponseCookie = build(REFRESH_TOKEN_COOKIE, token, jwtProperties.token.durations.refresh / 1000)

    fun expiredRefreshTokenCookie(): ResponseCookie = build(REFRESH_TOKEN_COOKIE, "", 0)

    private fun build(
        name: String,
        value: String,
        maxAgeSeconds: Long,
    ): ResponseCookie =
        ResponseCookie
            .from(name, value)
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .path("/")
            .maxAge(maxAgeSeconds)
            .build()

    companion object {
        const val OIDC_AUTH_COOKIE = "OIDC_AUTH_TOKEN"
        const val REFRESH_TOKEN_COOKIE = "REFRESH_TOKEN"
    }
}
