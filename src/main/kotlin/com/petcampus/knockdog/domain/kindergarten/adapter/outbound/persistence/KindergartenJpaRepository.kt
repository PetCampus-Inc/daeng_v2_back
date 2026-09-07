package com.petcampus.knockdog.domain.kindergarten.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface KindergartenJpaRepository : JpaRepository<KindergartenJpaEntity, Long> {
    fun findByNaverPlaceId(naverPlaceId: String): KindergartenJpaEntity?
}
