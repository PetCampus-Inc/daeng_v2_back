package com.petcampus.knockdog.domain.auth.adapter.outbound.oidc

import com.petcampus.knockdog.domain.auth.application.AuthException
import com.petcampus.knockdog.domain.auth.application.port.output.OidcIdentity
import com.petcampus.knockdog.domain.auth.domain.Provider
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

        val subject = claims.subject ?: throw AuthException.tokenVerificationFailed()
        val email = claims.get("email", String::class.java) ?: throw AuthException.tokenVerificationFailed()

        return OidcIdentity(provider, subject, email)
    }
}
