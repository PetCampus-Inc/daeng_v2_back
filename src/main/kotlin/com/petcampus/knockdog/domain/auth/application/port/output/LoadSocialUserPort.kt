package com.petcampus.knockdog.domain.auth.application.port.output

import com.petcampus.knockdog.domain.auth.domain.Provider
import com.petcampus.knockdog.domain.auth.domain.SocialUser

interface LoadSocialUserPort {
    fun findByProviderAndProviderId(
        provider: Provider,
        providerId: String,
    ): SocialUser?

    fun findAllByEmail(email: String): List<SocialUser>
}
