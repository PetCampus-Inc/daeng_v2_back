package com.petcampus.knockdog.domain.owner.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface OwnerJpaRepository : JpaRepository<OwnerJpaEntity, String>
