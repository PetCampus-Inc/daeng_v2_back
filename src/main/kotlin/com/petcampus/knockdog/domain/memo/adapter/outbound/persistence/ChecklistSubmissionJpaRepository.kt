package com.petcampus.knockdog.domain.memo.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface ChecklistSubmissionJpaRepository : JpaRepository<ChecklistSubmissionJpaEntity, Long> {
    fun findByUserCodeAndTargetId(
        userCode: String,
        targetId: String,
    ): ChecklistSubmissionJpaEntity?
}
