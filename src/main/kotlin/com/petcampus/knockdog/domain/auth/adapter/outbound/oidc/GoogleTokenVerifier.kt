package com.petcampus.knockdog.domain.auth.adapter.outbound.oidc

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.petcampus.knockdog.domain.auth.application.AuthErrorCode
import com.petcampus.knockdog.domain.auth.application.port.output.OidcIdentity
import com.petcampus.knockdog.domain.auth.domain.Provider
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.GeneralSecurityException

/** Google IDToken 검증기. 레거시 `GoogleTokenVerifier`와 동일하게 `google-api-client`를 사용(ADR 0006). */
@Component
class GoogleTokenVerifier(
    @Value("\${oauth.google.client-id.android}") androidClientId: String,
    @Value("\${oauth.google.client-id.ios}") iosClientId: String,
    @Value("\${oauth.google.client-id.web}") webClientId: String,
) : ProviderTokenVerifier {
    override val provider = Provider.GOOGLE

    private val googleIdTokenVerifier =
        GoogleIdTokenVerifier
            .Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(listOf(androidClientId, iosClientId, webClientId))
            .build()

    override fun verify(idToken: String): OidcIdentity {
        val googleIdToken =
            try {
                googleIdTokenVerifier.verify(idToken)
            } catch (e: GeneralSecurityException) {
                throw BusinessException(AuthErrorCode.EXTERNAL_SERVER_ERROR)
            } catch (e: java.io.IOException) {
                throw BusinessException(AuthErrorCode.EXTERNAL_SERVER_ERROR)
            } ?: throw BusinessException(AuthErrorCode.TOKEN_VERIFICATION_FAILED)

        val payload = googleIdToken.payload

        return OidcIdentity(provider, payload.subject, payload.email)
    }
}
