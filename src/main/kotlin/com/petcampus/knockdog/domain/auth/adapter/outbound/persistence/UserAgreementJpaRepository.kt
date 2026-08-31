package com.petcampus.knockdog.domain.auth.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface UserAgreementJpaRepository : JpaRepository<UserAgreementJpaEntity, Long> {
    fun findAllByUserId(userId: Long): List<UserAgreementJpaEntity>
}
