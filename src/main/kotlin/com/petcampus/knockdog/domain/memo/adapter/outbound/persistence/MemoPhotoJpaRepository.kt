package com.petcampus.knockdog.domain.memo.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface MemoPhotoJpaRepository : JpaRepository<MemoPhotoJpaEntity, Long> {
    fun findAllByUserCodeAndTargetIdOrderBySortOrder(
        userCode: String,
        targetId: String,
    ): List<MemoPhotoJpaEntity>

    fun countByUserCodeAndTargetId(
        userCode: String,
        targetId: String,
    ): Int
}
