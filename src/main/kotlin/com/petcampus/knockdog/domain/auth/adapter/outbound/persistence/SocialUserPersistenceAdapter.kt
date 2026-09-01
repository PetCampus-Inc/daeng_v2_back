package com.petcampus.knockdog.domain.auth.adapter.outbound.persistence

import com.petcampus.knockdog.domain.auth.application.port.output.LoadSocialUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.SaveSocialUserPort
import com.petcampus.knockdog.domain.auth.domain.Provider
import com.petcampus.knockdog.domain.auth.domain.SocialUser
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
class SocialUserPersistenceAdapter(
    private val socialUserJpaRepository: SocialUserJpaRepository,
    private val entityManager: EntityManager,
) : LoadSocialUserPort,
    SaveSocialUserPort {
    override fun save(socialUser: SocialUser): SocialUser {
        val userRef = socialUser.userId?.let { entityManager.getReference(UserJpaEntity::class.java, it.value) }
        return socialUserJpaRepository.save(socialUser.toJpaEntity(userRef)).toDomain()
    }

    override fun findByProviderAndProviderId(
        provider: Provider,
        providerId: String,
    ): SocialUser? = socialUserJpaRepository.findByProviderAndProviderId(provider, providerId)?.toDomain()

    override fun findAllByEmail(email: String): List<SocialUser> = socialUserJpaRepository.findAllByEmail(email).map { it.toDomain() }
}
