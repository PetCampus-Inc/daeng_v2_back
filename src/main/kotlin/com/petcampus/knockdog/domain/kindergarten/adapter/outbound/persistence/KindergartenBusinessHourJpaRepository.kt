package com.petcampus.knockdog.domain.kindergarten.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface KindergartenBusinessHourJpaRepository : JpaRepository<KindergartenBusinessHourJpaEntity, Long> {
    fun findAllByKindergartenId(kindergartenId: Long): List<KindergartenBusinessHourJpaEntity>
}
