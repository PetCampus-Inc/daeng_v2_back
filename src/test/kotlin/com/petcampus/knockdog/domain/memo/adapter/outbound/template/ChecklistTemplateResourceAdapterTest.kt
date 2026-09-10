package com.petcampus.knockdog.domain.memo.adapter.outbound.template

import com.petcampus.knockdog.domain.memo.domain.ChecklistQuestionType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ChecklistTemplateResourceAdapterTest {
    private val template = ChecklistTemplateResourceAdapter().load()

    @Test
    fun `5섹션 13문항, 섹션 순서 유지`() {
        assertEquals(5, template.sections.size)
        assertEquals(13, template.questionCodes.size)
        assertEquals("sec_register", template.sections.first().code)
        assertEquals("sec_policy", template.sections.last().code)
    }

    @Test
    fun `q_max_dogs_per_day는 INTEGER에 0부터 500 범위`() {
        val question = template.question("q_max_dogs_per_day")!!

        assertEquals(ChecklistQuestionType.INTEGER, question.type)
        assertEquals(0, question.min)
        assertEquals(500, question.max)
    }

    @Test
    fun `문항 ID 13개가 레거시·프론트 하드코딩 목록과 일치`() {
        assertEquals(
            setOf(
                "q_vaccine_proof_required",
                "q_neutered_required",
                "q_mixed_size_allowed",
                "q_manage_by_temper",
                "q_evaluate_temper",
                "q_schedule_by_temper",
                "q_max_dogs_per_day",
                "q_personalized_meal",
                "q_curriculum_timeblock",
                "q_nearby_vet",
                "q_accident_protocol",
                "q_trial_day_available",
                "q_refund_rules_clear",
            ),
            template.questionCodes,
        )
    }
}
