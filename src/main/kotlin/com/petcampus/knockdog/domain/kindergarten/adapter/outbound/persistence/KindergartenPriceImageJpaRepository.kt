package com.petcampus.knockdog.domain.kindergarten.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface KindergartenPriceImageJpaRepository : JpaRepository<KindergartenPriceImageJpaEntity, Long> {
    fun findAllByKindergartenIdOrderByDisplayOrder(kindergartenId: Long): List<KindergartenPriceImageJpaEntity>
}
