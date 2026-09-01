package com.petcampus.knockdog.domain.auth.adapter.outbound.oidc

import com.petcampus.knockdog.domain.auth.application.port.output.OidcIdentity
import com.petcampus.knockdog.domain.auth.domain.Provider

interface ProviderTokenVerifier {
    val provider: Provider

    fun verify(idToken: String): OidcIdentity
}
