package com.petcampus.knockdog.domain.auth.application.port.output

import com.petcampus.knockdog.domain.auth.domain.Provider

interface OidcVerifierPort {
    fun verify(
        provider: Provider,
        idToken: String,
    ): OidcIdentity
}

/** ID Token 검증 후 추출되는 신원 정보. name/picture는 토큰이 아니라 클라이언트 요청에서 별도로 받는다(레거시 동일). */
data class OidcIdentity(
    val provider: Provider,
    val providerId: String,
    val email: String,
)
