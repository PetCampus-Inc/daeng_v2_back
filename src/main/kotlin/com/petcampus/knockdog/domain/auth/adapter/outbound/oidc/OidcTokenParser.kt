package com.petcampus.knockdog.domain.auth.adapter.outbound.oidc

import com.petcampus.knockdog.domain.auth.application.AuthErrorCode
import com.petcampus.knockdog.global.exception.BusinessException
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Component
import java.security.PublicKey

@Component
class OidcTokenParser {
    fun parseClaims(
        token: String,
        publicKey: PublicKey,
    ): Claims =
        try {
            Jwts
                .parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: JwtException) {
            throw BusinessException(AuthErrorCode.TOKEN_VERIFICATION_FAILED)
        }
}
