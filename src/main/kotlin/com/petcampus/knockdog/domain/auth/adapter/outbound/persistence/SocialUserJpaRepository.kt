package com.petcampus.knockdog.domain.auth.adapter.outbound.persistence

import com.petcampus.knockdog.domain.auth.domain.Provider
import org.springframework.data.jpa.repository.JpaRepository

interface SocialUserJpaRepository : JpaRepository<SocialUserJpaEntity, Long> {
    fun findByProviderAndProviderId(
        provider: Provider,
        providerId: String,
    ): SocialUserJpaEntity?

    fun findAllByEmail(email: String): List<SocialUserJpaEntity>
}
