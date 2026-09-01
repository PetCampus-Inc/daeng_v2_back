package com.petcampus.knockdog.domain.auth.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<UserJpaEntity, Long> {
    fun findByUserCode(userCode: String): UserJpaEntity?
}
