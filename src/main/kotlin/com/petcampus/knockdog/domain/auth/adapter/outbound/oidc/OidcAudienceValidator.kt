package com.petcampus.knockdog.domain.auth.adapter.outbound.oidc

import com.petcampus.knockdog.domain.auth.application.AuthException
import io.jsonwebtoken.Claims

/**
 * 서명 검증만으로는 "이 앱이 아닌 다른 앱용으로 발급된, 진짜로 유효한 토큰"을 걸러낼 수 없다.
 * (동일 provider에 여러 앱이 등록돼 있으면 각 앱 사용자가 서로의 ID Token으로 로그인할 수 있게 되는
 * audience-confusion 문제 — 반드시 aud가 이 앱의 client-id 목록에 포함되는지 확인해야 한다.)
 */
object OidcAudienceValidator {
    fun validate(
        claims: Claims,
        expectedClientIds: List<String>,
    ) {
        if (expectedClientIds.isEmpty() || claims.audience.none { it in expectedClientIds }) {
            throw AuthException.tokenVerificationFailed()
        }
    }
}
