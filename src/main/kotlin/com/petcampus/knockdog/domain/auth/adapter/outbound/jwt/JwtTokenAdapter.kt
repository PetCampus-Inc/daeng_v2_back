package com.petcampus.knockdog.domain.auth.adapter.outbound.jwt

import com.petcampus.knockdog.domain.auth.application.AuthException
import com.petcampus.knockdog.domain.auth.application.port.output.OidcTokenClaims
import com.petcampus.knockdog.domain.auth.application.port.output.TokenPort
import com.petcampus.knockdog.domain.auth.domain.Provider
import com.petcampus.knockdog.domain.auth.domain.UserCode
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.crypto.SecretKey

/** ADR 0006: 커스텀 JWT 발급/검증 구조를 그대로 유지한다(`.oauth2Login()` 미사용). */
@Component
class JwtTokenAdapter(
    private val jwtProperties: JwtProperties,
) : TokenPort {
    private val key: SecretKey = Keys.hmacShaKeyFor(jwtProperties.secretKey.toByteArray(StandardCharsets.UTF_8))

    override fun issueOidcToken(
        provider: Provider,
        providerId: String,
        email: String,
    ): String {
        val claims = mapOf(CLAIM_TYPE to TYPE_OIDC_AUTH, CLAIM_PROVIDER to provider.name, CLAIM_EMAIL to email)
        return generateToken(claims, providerId, jwtProperties.token.durations.oidcAuth)
    }

    override fun parseOidcToken(token: String): OidcTokenClaims {
        val claims = parseClaims(token)
        validateType(claims, TYPE_OIDC_AUTH)

        val provider = Provider.valueOf(claims.get(CLAIM_PROVIDER, String::class.java))
        val email = claims.get(CLAIM_EMAIL, String::class.java)

        return OidcTokenClaims(provider, claims.subject, email)
    }

    override fun issueAccessToken(userCode: UserCode): String {
        val claims = mapOf(CLAIM_TYPE to TYPE_ACCESS)
        return generateToken(claims, userCode.value, jwtProperties.token.durations.access)
    }

    override fun issueRefreshToken(userCode: UserCode): String {
        val claims = mapOf(CLAIM_TYPE to TYPE_REFRESH)
        return generateToken(claims, userCode.value, jwtProperties.token.durations.refresh)
    }

    override fun parseAccessTokenSubject(token: String): UserCode {
        val claims = parseClaims(token)
        validateType(claims, TYPE_ACCESS)
        return UserCode(claims.subject)
    }

    private fun generateToken(
        claims: Map<String, Any>,
        subject: String,
        validityMs: Long,
    ): String {
        val now = Date()
        val expiry = Date(now.time + validityMs)

        return Jwts
            .builder()
            .claims(claims)
            .subject(subject)
            .issuer(jwtProperties.issuer)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key, Jwts.SIG.HS256)
            .compact()
    }

    private fun parseClaims(token: String): Claims =
        try {
            Jwts
                .parser()
                .verifyWith(key)
                .requireIssuer(jwtProperties.issuer)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: ExpiredJwtException) {
            throw AuthException.expiredToken()
        } catch (e: JwtException) {
            throw AuthException.invalidToken()
        }

    private fun validateType(
        claims: Claims,
        expected: String,
    ) {
        if (claims.get(CLAIM_TYPE, String::class.java) != expected) {
            throw AuthException.invalidToken()
        }
    }

    companion object {
        private const val CLAIM_TYPE = "type"
        private const val CLAIM_PROVIDER = "provider"
        private const val CLAIM_EMAIL = "email"
        private const val TYPE_OIDC_AUTH = "OIDC_AUTH"
        private const val TYPE_ACCESS = "ACCESS"
        private const val TYPE_REFRESH = "REFRESH"
    }
}
