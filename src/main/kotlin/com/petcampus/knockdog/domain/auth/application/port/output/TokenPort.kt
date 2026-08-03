package com.petcampus.knockdog.domain.auth.application.port.output

import com.petcampus.knockdog.domain.auth.domain.Provider
import com.petcampus.knockdog.domain.auth.domain.UserCode

/** 3종 토큰(OIDC 임시/액세스/리프레시)을 모두 발급·검증한다 (ADR 0006). */
interface TokenPort {
    fun issueOidcToken(
        provider: Provider,
        providerId: String,
        email: String,
    ): String

    fun parseOidcToken(token: String): OidcTokenClaims

    fun issueAccessToken(userCode: UserCode): String

    fun issueRefreshToken(userCode: UserCode): String

    /** 서명·만료·타입(ACCESS) 검증 후 subject(UserCode)를 반환한다. 인증 필터가 사용한다. */
    fun parseAccessTokenSubject(token: String): UserCode
}

data class OidcTokenClaims(
    val provider: Provider,
    val providerId: String,
    val email: String,
)
