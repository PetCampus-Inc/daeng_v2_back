package com.petcampus.knockdog.domain.memo.application.port.output

import com.petcampus.knockdog.domain.memo.domain.ChecklistSubmission

interface LoadChecklistSubmissionPort {
    fun findByUserCodeAndTargetId(
        userCode: String,
        targetId: String,
    ): ChecklistSubmission?
}
