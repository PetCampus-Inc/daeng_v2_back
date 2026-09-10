package com.petcampus.knockdog.domain.memo.adapter.outbound.persistence

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
        checklistSubmissionJpaRepository.upsert(
            userCode = submission.userCode,
            targetId = submission.targetId,
            templateVersion = submission.templateVersion,
            answers = objectMapper.writeValueAsString(submission.answers),
        )
        return requireNotNull(
            checklistSubmissionJpaRepository.findByUserCodeAndTargetId(submission.userCode, submission.targetId),
        ).toDomain()
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()
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
