package com.petcampus.knockdog.domain.pet.adapter.outbound.persistence

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PetJpaRepository : JpaRepository<PetJpaEntity, Long> {
    @Query("select p from PetJpaEntity p where p.user.id = :userId and p.deletedAt is null")
    fun findAllActiveByUserId(
        @Param("userId") userId: Long,
    ): List<PetJpaEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PetJpaEntity p where p.user.id = :userId and p.deletedAt is null")
    fun findAllActiveByUserIdForUpdate(
        @Param("userId") userId: Long,
    ): List<PetJpaEntity>
}
