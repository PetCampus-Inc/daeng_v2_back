package com.petcampus.knockdog.domain.memo.domain

class ChecklistSubmission private constructor(
    val id: Long?,
    val userCode: String,
    val targetId: String,
    val templateVersion: String,
    val answers: Map<String, String>,
) {
    fun withAnswers(
        templateVersion: String,
        answers: Map<String, String>,
    ): ChecklistSubmission = ChecklistSubmission(id, userCode, targetId, templateVersion, answers)

    companion object {
        fun create(
            userCode: String,
            targetId: String,
            templateVersion: String,
            answers: Map<String, String>,
        ): ChecklistSubmission = ChecklistSubmission(null, userCode, targetId, templateVersion, answers)

        fun reconstitute(
            id: Long,
            userCode: String,
            targetId: String,
            templateVersion: String,
            answers: Map<String, String>,
        ): ChecklistSubmission = ChecklistSubmission(id, userCode, targetId, templateVersion, answers)
    }
}
