package com.petcampus.knockdog.domain.memo.application.service

import com.petcampus.knockdog.domain.memo.application.MemoErrorCode
import com.petcampus.knockdog.domain.memo.application.port.input.SaveChecklistAnswersCommand
import com.petcampus.knockdog.domain.memo.application.port.output.LoadChecklistSubmissionPort
import com.petcampus.knockdog.domain.memo.application.port.output.LoadChecklistTemplatePort
import com.petcampus.knockdog.domain.memo.application.port.output.SaveChecklistSubmissionPort
import com.petcampus.knockdog.domain.memo.domain.ChecklistQuestion
import com.petcampus.knockdog.domain.memo.domain.ChecklistQuestionType
import com.petcampus.knockdog.domain.memo.domain.ChecklistSection
import com.petcampus.knockdog.domain.memo.domain.ChecklistSubmission
import com.petcampus.knockdog.domain.memo.domain.ChecklistTemplate
import com.petcampus.knockdog.global.exception.BusinessException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChecklistServiceTest {
    private val template =
        ChecklistTemplate(
            "registration",
            "1",
            "ko-KR",
            "등록 체크리스트",
            listOf(
                ChecklistSection(
                    "sec_register",
                    "등록요건",
                    listOf(
                        ChecklistQuestion(
                            "q_vaccine_proof_required",
                            "백신 접종증명서를 제출해야 하나요?",
                            ChecklistQuestionType.TRI_STATE,
                            null,
                            null,
                        ),
                        ChecklistQuestion(
                            "q_max_dogs_per_day",
                            "하루에 총 몇 마리까지 등원하나요?",
                            ChecklistQuestionType.INTEGER,
                            0,
                            500,
                        ),
                    ),
                ),
            ),
        )

    private val templatePort =
        object : LoadChecklistTemplatePort {
            override fun load() = template
        }

    private class FakeSubmissionPort :
        LoadChecklistSubmissionPort,
        SaveChecklistSubmissionPort {
        val store = mutableMapOf<Pair<String, String>, ChecklistSubmission>()
        private var seq = 1L

        override fun findByUserCodeAndTargetId(
            userCode: String,
            targetId: String,
        ) = store[userCode to targetId]

        override fun save(submission: ChecklistSubmission): ChecklistSubmission {
            val persisted =
                if (submission.id == null) {
                    ChecklistSubmission.reconstitute(
                        seq++,
                        submission.userCode,
                        submission.targetId,
                        submission.templateVersion,
                        submission.answers,
                    )
                } else {
                    submission
                }
            store[persisted.userCode to persisted.targetId] = persisted
            return persisted
        }
    }

    private fun service(port: FakeSubmissionPort = FakeSubmissionPort()) = ChecklistService(templatePort, port, port)

    @Test
    fun `저장된 게 없으면 sections 빈 리스트`() {
        assertEquals(emptyList(), service().get("A1B2C3D4", "place-1").sections)
    }

    @Test
    fun `save는 값을 정규화해 저장하고 템플릿 순서로 조회된다`() {
        val svc = service()
        svc.save(
            SaveChecklistAnswersCommand(
                "A1B2C3D4",
                "place-1",
                listOf(
                    SaveChecklistAnswersCommand.AnswerInput("q_vaccine_proof_required", "yes"),
                    SaveChecklistAnswersCommand.AnswerInput("q_max_dogs_per_day", "30"),
                ),
            ),
        )

        val view = svc.get("A1B2C3D4", "place-1")
        assertEquals(1, view.sections.size)
        assertEquals("sec_register", view.sections[0].sectionId)
        assertEquals("YES", view.sections[0].answers[0].value)
        assertEquals("30", view.sections[0].answers[1].value)
        assertEquals("백신 접종증명서를 제출해야 하나요?", view.sections[0].answers[0].question)
    }

    @Test
    fun `모르는 questionId면 MEMO_INVALID_CHECKLIST_ANSWER`() {
        val exception =
            assertFailsWith<BusinessException> {
                service().save(
                    SaveChecklistAnswersCommand(
                        "A1B2C3D4",
                        "p",
                        listOf(SaveChecklistAnswersCommand.AnswerInput("q_nope", "YES")),
                    ),
                )
            }
        assertEquals(MemoErrorCode.INVALID_CHECKLIST_ANSWER, exception.errorCode)
    }

    @Test
    fun `TRI_STATE에 숫자를 주면 거부, INTEGER 범위 밖도 거부`() {
        assertFailsWith<BusinessException> {
            service().save(
                SaveChecklistAnswersCommand(
                    "A1B2C3D4",
                    "p",
                    listOf(SaveChecklistAnswersCommand.AnswerInput("q_vaccine_proof_required", "5")),
                ),
            )
        }
        assertFailsWith<BusinessException> {
            service().save(
                SaveChecklistAnswersCommand(
                    "A1B2C3D4",
                    "p",
                    listOf(SaveChecklistAnswersCommand.AnswerInput("q_max_dogs_per_day", "999")),
                ),
            )
        }
    }

    @Test
    fun `PUT은 전체 교체 — 이전 답변 중 빠진 문항은 사라진다`() {
        val svc = service()
        svc.save(
            SaveChecklistAnswersCommand(
                "A1B2C3D4",
                "p",
                listOf(
                    SaveChecklistAnswersCommand.AnswerInput("q_vaccine_proof_required", "YES"),
                    SaveChecklistAnswersCommand.AnswerInput("q_max_dogs_per_day", "10"),
                ),
            ),
        )
        svc.save(
            SaveChecklistAnswersCommand(
                "A1B2C3D4",
                "p",
                listOf(SaveChecklistAnswersCommand.AnswerInput("q_vaccine_proof_required", "NO")),
            ),
        )

        val view = svc.get("A1B2C3D4", "p")
        assertEquals(1, view.sections[0].answers.size)
        assertEquals("NO", view.sections[0].answers[0].value)
    }
}
