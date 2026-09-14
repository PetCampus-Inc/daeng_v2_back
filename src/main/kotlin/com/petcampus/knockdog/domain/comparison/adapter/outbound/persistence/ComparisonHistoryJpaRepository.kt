package com.petcampus.knockdog.domain.comparison.adapter.outbound.persistence

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ComparisonHistoryJpaRepository : JpaRepository<ComparisonHistoryJpaEntity, Long> {
    fun findAllByUserCodeAndDeletedAtIsNullOrderByUpdatedAtDescIdDesc(
        userCode: String,
        pageable: Pageable,
    ): List<ComparisonHistoryJpaEntity>

    fun findByIdAndDeletedAtIsNull(id: Long): ComparisonHistoryJpaEntity?

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        nativeQuery = true,
        value =
            "INSERT INTO comparison_histories " +
                "(user_code, kindergarten_id_a, kindergarten_id_b, created_at, updated_at) " +
                "VALUES (:userCode, :kindergartenIdA, :kindergartenIdB, NOW(6), NOW(6)) " +
                "ON DUPLICATE KEY UPDATE updated_at = NOW(6), deleted_at = NULL",
    )
    fun upsert(
        @Param("userCode") userCode: String,
        @Param("kindergartenIdA") kindergartenIdA: String,
        @Param("kindergartenIdB") kindergartenIdB: String,
    )

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        nativeQuery = true,
        value = "UPDATE comparison_histories SET deleted_at = NOW(6) WHERE id = :id AND deleted_at IS NULL",
    )
    fun softDeleteById(
        @Param("id") id: Long,
    )
}
