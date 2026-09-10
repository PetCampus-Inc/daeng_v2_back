package com.petcampus.knockdog.domain.memo.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChecklistTemplateTest {
    private val triState = ChecklistQuestion("q1", "라벨", ChecklistQuestionType.TRI_STATE, null, null)
    private val integer = ChecklistQuestion("q2", "라벨", ChecklistQuestionType.INTEGER, 0, 500)

    @Test
    fun `TRI_STATE는 YES NO UNKNOWN만 허용하고 대문자로 정규화`() {
        assertEquals("YES", triState.normalize("yes"))
        assertEquals("NO", triState.normalize("NO"))
        assertEquals("UNKNOWN", triState.normalize("unknown"))
    }

    @Test
    fun `TRI_STATE에 엉뚱한 값이면 null`() {
        assertNull(triState.normalize("MAYBE"))
        assertNull(triState.normalize("5"))
    }

    @Test
    fun `INTEGER는 범위 안 정수 문자열만 허용`() {
        assertEquals("30", integer.normalize("30"))
        assertNull(integer.normalize("501"))
        assertNull(integer.normalize("-1"))
        assertNull(integer.normalize("3.5"))
        assertNull(integer.normalize("abc"))
    }

    @Test
    fun `template question 조회와 questionCodes`() {
        val template =
            ChecklistTemplate(
                "registration",
                "1",
                "ko-KR",
                "등록 체크리스트",
                listOf(ChecklistSection("s1", "섹션", listOf(triState, integer))),
            )

        assertEquals(triState, template.question("q1"))
        assertNull(template.question("nope"))
        assertEquals(setOf("q1", "q2"), template.questionCodes)
    }
}
