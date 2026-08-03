package com.petcampus.knockdog.domain.auth.adapter.outbound.oidc

import com.petcampus.knockdog.domain.auth.application.AuthException
import com.petcampus.knockdog.domain.auth.application.port.output.OidcIdentity
import com.petcampus.knockdog.domain.auth.application.port.output.OidcVerifierPort
import com.petcampus.knockdog.domain.auth.domain.Provider
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
        val verifier = verifierByProvider[provider] ?: throw AuthException.invalidProvider(provider.name)
        return verifier.verify(idToken)
    }
}
