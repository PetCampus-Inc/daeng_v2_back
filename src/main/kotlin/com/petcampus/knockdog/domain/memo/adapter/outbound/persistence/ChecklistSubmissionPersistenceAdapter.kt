package com.petcampus.knockdog.domain.memo.adapter.outbound.persistence

import com.petcampus.knockdog.domain.memo.application.port.output.LoadChecklistSubmissionPort
import com.petcampus.knockdog.domain.memo.application.port.output.SaveChecklistSubmissionPort
import com.petcampus.knockdog.domain.memo.domain.ChecklistSubmission
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ChecklistSubmissionPersistenceAdapter(
    private val checklistSubmissionJpaRepository: ChecklistSubmissionJpaRepository,
) : LoadChecklistSubmissionPort,
    SaveChecklistSubmissionPort {
    @Transactional(readOnly = true)
    override fun findByUserCodeAndTargetId(
        userCode: String,
        targetId: String,
    ): ChecklistSubmission? = checklistSubmissionJpaRepository.findByUserCodeAndTargetId(userCode, targetId)?.toDomain()

    @Transactional
    override fun save(submission: ChecklistSubmission): ChecklistSubmission {
        val entity =
            submission.id?.let { id ->
                checklistSubmissionJpaRepository.findById(id).orElseThrow().apply {
                    templateVersion = submission.templateVersion
                    answers = submission.answers
                }
            } ?: ChecklistSubmissionJpaEntity(
                userCode = submission.userCode,
                targetId = submission.targetId,
                templateVersion = submission.templateVersion,
                answers = submission.answers,
            )
        return checklistSubmissionJpaRepository.save(entity).toDomain()
    }
}

private fun ChecklistSubmissionJpaEntity.toDomain(): ChecklistSubmission =
    ChecklistSubmission.reconstitute(
        id = requireNotNull(id),
        userCode = userCode,
        targetId = targetId,
        templateVersion = templateVersion,
        answers = answers,
    )
