package com.petcampus.knockdog.domain.auth.adapter.outbound.oidc

import com.petcampus.knockdog.domain.auth.application.AuthException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/** audience-confusion 방지 로직 단위 테스트 (다른 앱용으로 발급된, 서명은 진짜인 토큰을 걸러내는지). */
class OidcAudienceValidatorTest {
    private val key = Keys.hmacShaKeyFor(ByteArray(32) { it.toByte() })

    private fun claimsWithAudience(vararg audience: String) =
        Jwts
            .parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(
                Jwts
                    .builder()
                    .audience()
                    .add(audience.toList())
                    .and()
                    .signWith(key)
                    .compact(),
            ).payload

    @Test
    fun `aud가 설정된 client-id 목록에 있으면 통과한다`() {
        val claims = claimsWithAudience("client-a")

        OidcAudienceValidator.validate(claims, listOf("client-a", "client-b"))
    }

    @Test
    fun `aud가 설정된 client-id 목록에 없으면 예외가 발생한다`() {
        val claims = claimsWithAudience("attacker-app")

        assertFailsWith<AuthException> {
            OidcAudienceValidator.validate(claims, listOf("client-a"))
        }
    }

    @Test
    fun `client-id가 설정돼 있지 않으면 무조건 예외가 발생한다 (fail closed)`() {
        val claims = claimsWithAudience("client-a")

        assertFailsWith<AuthException> {
            OidcAudienceValidator.validate(claims, emptyList())
        }
    }
}
