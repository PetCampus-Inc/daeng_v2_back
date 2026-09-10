package com.petcampus.knockdog.domain.memo.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MemoTest {
    @Test
    fun `create는 content가 없어도 만들어진다`() {
        val memo = Memo.create(userCode = "A1B2C3D4", targetId = "1234567890", content = null)

        assertNull(memo.content)
        assertNull(memo.id)
    }

    @Test
    fun `content가 2000자를 넘으면 불변식 위반`() {
        assertFailsWith<IllegalArgumentException> {
            Memo.create("A1B2C3D4", "1234567890", "가".repeat(2001))
        }
    }

    @Test
    fun `content가 정확히 2000자면 통과`() {
        val memo = Memo.create("A1B2C3D4", "1234567890", "가".repeat(2000))

        assertEquals(2000, memo.content!!.length)
    }

    @Test
    fun `withContent는 id를 유지한 채 content만 바꾼다`() {
        val original = Memo.reconstitute(MemoId(7L), "A1B2C3D4", "1234567890", "old")

        val updated = original.withContent("new")

        assertEquals("new", updated.content)
        assertEquals(MemoId(7L), updated.id)
    }
}
