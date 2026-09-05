package com.petcampus.knockdog.domain.kindergarten.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface KindergartenCategoryJpaRepository : JpaRepository<KindergartenCategoryJpaEntity, Long> {
    fun findAllByKindergartenId(kindergartenId: Long): List<KindergartenCategoryJpaEntity>
}
