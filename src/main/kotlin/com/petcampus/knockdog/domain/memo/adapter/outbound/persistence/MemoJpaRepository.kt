package com.petcampus.knockdog.domain.memo.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MemoJpaRepository : JpaRepository<MemoJpaEntity, Long> {
    fun findByUserCodeAndTargetId(
        userCode: String,
        targetId: String,
    ): MemoJpaEntity?

    fun findAllByUserCodeOrderByUpdatedAtDescIdDesc(userCode: String): List<MemoJpaEntity>

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        nativeQuery = true,
        value =
            "INSERT INTO memos (user_code, target_id, content, created_at, updated_at) " +
                "VALUES (:userCode, :targetId, :content, NOW(6), NOW(6)) " +
                "ON DUPLICATE KEY UPDATE content = :content, updated_at = NOW(6)",
    )
    fun upsert(
        @Param("userCode") userCode: String,
        @Param("targetId") targetId: String,
        @Param("content") content: String?,
    )
}
