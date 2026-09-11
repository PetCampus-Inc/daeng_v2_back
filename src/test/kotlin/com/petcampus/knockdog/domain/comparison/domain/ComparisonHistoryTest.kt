package com.petcampus.knockdog.domain.comparison.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ComparisonHistoryTest {
    @Test
    fun `두 유치원 ID를 사전순으로 정렬해 보관한다`() {
        val history = ComparisonHistory.create("A1B2C3D4", "naver-9", "naver-2")

        assertEquals("naver-2", history.kindergartenIdA)
        assertEquals("naver-9", history.kindergartenIdB)
    }

    @Test
    fun `naverPlaceIds는 정렬된 순서로 두 ID를 돌려준다`() {
        val history = ComparisonHistory.create("A1B2C3D4", "naver-9", "naver-2")

        assertEquals(listOf("naver-2", "naver-9"), history.naverPlaceIds)
    }

    @Test
    fun `같은 유치원 두 개면 생성할 수 없다`() {
        assertFailsWith<IllegalArgumentException> {
            ComparisonHistory.create("A1B2C3D4", "naver-1", "naver-1")
        }
    }
}
