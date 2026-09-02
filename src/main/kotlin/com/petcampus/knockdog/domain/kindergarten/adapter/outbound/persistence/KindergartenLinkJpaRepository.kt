package com.petcampus.knockdog.domain.kindergarten.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface KindergartenLinkJpaRepository : JpaRepository<KindergartenLinkJpaEntity, Long> {
    fun findAllByKindergartenId(kindergartenId: Long): List<KindergartenLinkJpaEntity>
}
