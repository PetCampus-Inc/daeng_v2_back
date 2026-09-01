package com.petcampus.knockdog.domain.auth.adapter.outbound.persistence

import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserAgreementPort
import com.petcampus.knockdog.domain.auth.application.port.output.SaveUserAgreementPort
import com.petcampus.knockdog.domain.auth.domain.AgreementTermType
import com.petcampus.knockdog.domain.auth.domain.UserAgreement
import com.petcampus.knockdog.domain.auth.domain.UserId
import org.springframework.stereotype.Component

@Component
class UserAgreementPersistenceAdapter(
    private val userAgreementJpaRepository: UserAgreementJpaRepository,
) : LoadUserAgreementPort,
    SaveUserAgreementPort {
    override fun findTermTypesByUserId(userId: UserId): Set<AgreementTermType> =
        userAgreementJpaRepository.findAllByUserId(userId.value).map { it.termType }.toSet()

    override fun saveAll(agreements: List<UserAgreement>) {
        userAgreementJpaRepository.saveAll(agreements.map { it.toJpaEntity() })
    }
}

private fun UserAgreement.toJpaEntity(): UserAgreementJpaEntity =
    UserAgreementJpaEntity(
        userId = userId.value,
        termType = termType,
        agreedAt = agreedAt,
    )
