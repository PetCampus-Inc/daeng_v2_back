package com.petcampus.knockdog.domain.memo.application.port.output

import com.petcampus.knockdog.domain.memo.domain.ChecklistSubmission

interface SaveChecklistSubmissionPort {
    fun save(submission: ChecklistSubmission): ChecklistSubmission
}
