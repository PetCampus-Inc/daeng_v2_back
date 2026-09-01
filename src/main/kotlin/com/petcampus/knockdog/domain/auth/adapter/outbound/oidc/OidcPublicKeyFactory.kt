package com.petcampus.knockdog.domain.auth.adapter.outbound.oidc

import com.petcampus.knockdog.domain.auth.application.AuthErrorCode
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64

@Component
class OidcPublicKeyFactory {
    fun generatePublicKey(
        tokenHeaders: Map<String, String>,
        publicKeys: OidcPublicKeyList,
    ): PublicKey {
        val matched = publicKeys.getMatchedKey(tokenHeaders["kid"], tokenHeaders["alg"])

        val nBytes = Base64.getUrlDecoder().decode(matched.n)
        val eBytes = Base64.getUrlDecoder().decode(matched.e)
        val spec = RSAPublicKeySpec(BigInteger(1, nBytes), BigInteger(1, eBytes))

        return try {
            KeyFactory.getInstance(matched.kty).generatePublic(spec)
        } catch (e: GeneralSecurityException) {
            throw BusinessException(AuthErrorCode.TOKEN_VERIFICATION_FAILED)
        }
    }
}
