package com.petcampus.knockdog.domain.kindergarten.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface KindergartenOptionJpaRepository : JpaRepository<KindergartenOptionJpaEntity, Long> {
    fun findAllByKindergartenId(kindergartenId: Long): List<KindergartenOptionJpaEntity>
}
