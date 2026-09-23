package com.petcampus.knockdog.domain.bookmark.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface BookmarkJpaRepository : JpaRepository<BookmarkJpaEntity, Long> {
    fun findAllByUserCodeOrderByCreatedAtDescIdDesc(userCode: String): List<BookmarkJpaEntity>

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        nativeQuery = true,
        value =
            "INSERT INTO bookmarks (user_code, kindergarten_id, created_at, updated_at) " +
                "VALUES (:userCode, :kindergartenId, NOW(6), NOW(6)) " +
                "ON DUPLICATE KEY UPDATE id = id",
    )
    fun createIfAbsent(
        @Param("userCode") userCode: String,
        @Param("kindergartenId") kindergartenId: String,
    )

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        "DELETE FROM BookmarkJpaEntity b WHERE b.userCode = :userCode AND b.kindergartenId = :kindergartenId",
    )
    fun deleteByUserCodeAndKindergartenId(
        @Param("userCode") userCode: String,
        @Param("kindergartenId") kindergartenId: String,
    ): Int
}
