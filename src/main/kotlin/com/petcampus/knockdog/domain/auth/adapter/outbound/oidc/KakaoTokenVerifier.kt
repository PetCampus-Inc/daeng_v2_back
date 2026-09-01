package com.petcampus.knockdog.domain.auth.adapter.outbound.oidc

import com.petcampus.knockdog.domain.auth.application.AuthErrorCode
import com.petcampus.knockdog.domain.auth.application.port.output.OidcIdentity
import com.petcampus.knockdog.domain.auth.domain.Provider
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/** Kakao IDToken 검증기. 레거시 `KakaoTokenVerifier` 로직 그대로 이관(ADR 0006). */
@Component
class KakaoTokenVerifier(
    private val oidcPublicKeyClient: OidcPublicKeyClient,
    private val oidcPublicKeyFactory: OidcPublicKeyFactory,
    private val oidcTokenParser: OidcTokenParser,
    private val jwtHeaderParser: JwtHeaderParser,
    @param:Value("\${oauth.kakao.public-key-url}") private val publicKeyUrl: String,
    @param:Value("\${oauth.kakao.client-ids:}") private val clientIdsProperty: String,
) : ProviderTokenVerifier {
    override val provider = Provider.KAKAO
    private val clientIds = clientIdsProperty.split(",").map { it.trim() }.filter { it.isNotBlank() }

    override fun verify(idToken: String): OidcIdentity {
        val headers = jwtHeaderParser.parseHeaders(idToken)
        val publicKeys = oidcPublicKeyClient.getPublicKeys(provider, publicKeyUrl)
        val publicKey = oidcPublicKeyFactory.generatePublicKey(headers, publicKeys)
        val claims = oidcTokenParser.parseClaims(idToken, publicKey)
        OidcAudienceValidator.validate(claims, clientIds)

        val subject = claims.subject ?: throw BusinessException(AuthErrorCode.TOKEN_VERIFICATION_FAILED)
        val email = claims.get("email", String::class.java) ?: throw BusinessException(AuthErrorCode.TOKEN_VERIFICATION_FAILED)

        return OidcIdentity(provider, subject, email)
    }
}
