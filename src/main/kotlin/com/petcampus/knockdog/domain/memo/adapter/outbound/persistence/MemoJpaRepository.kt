package com.petcampus.knockdog.domain.memo.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface MemoJpaRepository : JpaRepository<MemoJpaEntity, Long> {
    fun findByUserCodeAndTargetId(
        userCode: String,
        targetId: String,
    ): MemoJpaEntity?

    fun findAllByUserCodeOrderByUpdatedAtDesc(userCode: String): List<MemoJpaEntity>
}
