package com.petcampus.knockdog.domain.auth.adapter.outbound.oidc

import com.petcampus.knockdog.domain.auth.application.AuthErrorCode
import com.petcampus.knockdog.domain.auth.application.port.output.OidcIdentity
import com.petcampus.knockdog.domain.auth.application.port.output.OidcVerifierPort
import com.petcampus.knockdog.domain.auth.domain.Provider
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.stereotype.Component

@Component
class OidcVerifierAdapter(
    verifiers: List<ProviderTokenVerifier>,
) : OidcVerifierPort {
    private val verifierByProvider: Map<Provider, ProviderTokenVerifier> = verifiers.associateBy { it.provider }

    override fun verify(
        provider: Provider,
        idToken: String,
    ): OidcIdentity {
        val verifier =
            verifierByProvider[provider]
                ?: throw BusinessException(AuthErrorCode.INVALID_PROVIDER, "지원하지 않는 provider입니다: ${provider.name}")
        return verifier.verify(idToken)
    }
}
