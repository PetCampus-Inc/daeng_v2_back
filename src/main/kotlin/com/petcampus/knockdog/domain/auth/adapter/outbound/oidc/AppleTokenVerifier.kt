package com.petcampus.knockdog.domain.auth.adapter.outbound.oidc

import com.petcampus.knockdog.domain.auth.application.AuthException
import com.petcampus.knockdog.domain.auth.application.port.output.OidcIdentity
import com.petcampus.knockdog.domain.auth.domain.Provider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/** Apple IDToken 검증기. 레거시 `AppleTokenVerifier` 로직 그대로 이관(ADR 0006). */
@Component
class AppleTokenVerifier(
    private val oidcPublicKeyClient: OidcPublicKeyClient,
    private val oidcPublicKeyFactory: OidcPublicKeyFactory,
    private val oidcTokenParser: OidcTokenParser,
    private val jwtHeaderParser: JwtHeaderParser,
    @param:Value("\${oauth.apple.public-key-url}") private val publicKeyUrl: String,
    @param:Value("\${oauth.apple.client-ids:}") private val clientIdsProperty: String,
) : ProviderTokenVerifier {
    override val provider = Provider.APPLE
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
