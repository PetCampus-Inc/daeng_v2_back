package com.petcampus.knockdog.domain.memo.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface MemoPhotoJpaRepository : JpaRepository<MemoPhotoJpaEntity, Long> {
    fun findAllByMemoIdOrderBySortOrder(memoId: Long): List<MemoPhotoJpaEntity>

    fun deleteAllByMemoId(memoId: Long)
}
