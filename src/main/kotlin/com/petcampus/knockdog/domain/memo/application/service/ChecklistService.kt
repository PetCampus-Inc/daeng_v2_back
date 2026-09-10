package com.petcampus.knockdog.domain.memo.application.service

import com.petcampus.knockdog.domain.memo.application.MemoErrorCode
import com.petcampus.knockdog.domain.memo.application.port.input.ChecklistAnswersView
import com.petcampus.knockdog.domain.memo.application.port.input.ChecklistTemplateView
import com.petcampus.knockdog.domain.memo.application.port.input.GetChecklistAnswersUseCase
import com.petcampus.knockdog.domain.memo.application.port.input.GetChecklistTemplateUseCase
import com.petcampus.knockdog.domain.memo.application.port.input.SaveChecklistAnswersCommand
import com.petcampus.knockdog.domain.memo.application.port.input.SaveChecklistAnswersUseCase
import com.petcampus.knockdog.domain.memo.application.port.output.LoadChecklistSubmissionPort
import com.petcampus.knockdog.domain.memo.application.port.output.LoadChecklistTemplatePort
import com.petcampus.knockdog.domain.memo.application.port.output.SaveChecklistSubmissionPort
import com.petcampus.knockdog.domain.memo.domain.ChecklistSubmission
import com.petcampus.knockdog.domain.memo.domain.ChecklistTemplate
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChecklistService(
    private val loadChecklistTemplatePort: LoadChecklistTemplatePort,
    private val loadChecklistSubmissionPort: LoadChecklistSubmissionPort,
    private val saveChecklistSubmissionPort: SaveChecklistSubmissionPort,
) : GetChecklistTemplateUseCase,
    GetChecklistAnswersUseCase,
    SaveChecklistAnswersUseCase {
    override fun getTemplate(): ChecklistTemplateView = loadChecklistTemplatePort.load().toView()

    @Transactional
    override fun save(command: SaveChecklistAnswersCommand): ChecklistAnswersView {
        val template = loadChecklistTemplatePort.load()
        val normalized =
            command.answers.associate { input ->
                val question =
                    template.question(input.questionId)
                        ?: throw BusinessException(MemoErrorCode.INVALID_CHECKLIST_ANSWER)
                val value =
                    question.normalize(input.value)
                        ?: throw BusinessException(MemoErrorCode.INVALID_CHECKLIST_ANSWER)
                input.questionId to value
            }

        saveChecklistSubmissionPort.save(
            ChecklistSubmission.create(command.userCode, command.targetId, template.version, normalized),
        )

        return assembleAnswersView(template, normalized)
    }

    @Transactional(readOnly = true)
    override fun get(
        userCode: String,
        targetId: String,
    ): ChecklistAnswersView {
        val submission =
            loadChecklistSubmissionPort.findByUserCodeAndTargetId(userCode, targetId)
                ?: return ChecklistAnswersView(emptyList())
        return assembleAnswersView(loadChecklistTemplatePort.load(), submission.answers)
    }

    private fun assembleAnswersView(
        template: ChecklistTemplate,
        answers: Map<String, String>,
    ): ChecklistAnswersView =
        ChecklistAnswersView(
            sections =
                template.sections.mapNotNull { section ->
                    val items =
                        section.questions.mapNotNull { question ->
                            answers[question.code]?.let {
                                ChecklistAnswersView.AnswerItem(question.code, question.label, it)
                            }
                        }
                    if (items.isEmpty()) null else ChecklistAnswersView.SectionAnswers(section.code, section.title, items)
                },
        )
}

private fun ChecklistTemplate.toView() =
    ChecklistTemplateView(
        template = ChecklistTemplateView.Meta(code, version, locale, title),
        sections =
            sections.map { section ->
                ChecklistTemplateView.SectionView(
                    id = section.code,
                    title = section.title,
                    questions =
                        section.questions.map {
                            ChecklistTemplateView.QuestionView(it.code, it.label, it.type.name)
                        },
                )
            },
    )
