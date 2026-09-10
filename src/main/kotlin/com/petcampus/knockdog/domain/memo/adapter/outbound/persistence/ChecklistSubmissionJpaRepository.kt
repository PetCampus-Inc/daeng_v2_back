package com.petcampus.knockdog.domain.memo.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ChecklistSubmissionJpaRepository : JpaRepository<ChecklistSubmissionJpaEntity, Long> {
    fun findByUserCodeAndTargetId(
        userCode: String,
        targetId: String,
    ): ChecklistSubmissionJpaEntity?

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        nativeQuery = true,
        value =
            "INSERT INTO checklist_submissions (user_code, target_id, template_version, answers, created_at, updated_at) " +
                "VALUES (:userCode, :targetId, :templateVersion, :answers, NOW(6), NOW(6)) " +
                "ON DUPLICATE KEY UPDATE template_version = :templateVersion, answers = :answers, updated_at = NOW(6)",
    )
    fun upsert(
        @Param("userCode") userCode: String,
        @Param("targetId") targetId: String,
        @Param("templateVersion") templateVersion: String,
        @Param("answers") answers: String,
    )
}
