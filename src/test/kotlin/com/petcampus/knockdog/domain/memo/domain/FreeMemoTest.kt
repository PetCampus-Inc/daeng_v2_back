package com.petcampus.knockdog.domain.memo.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class FreeMemoTest {
    @Test
    fun `create는 content가 없어도 만들어진다`() {
        val memo = FreeMemo.create(userCode = "A1B2C3D4", targetId = "1234567890", content = null)

        assertNull(memo.content)
        assertNull(memo.id)
        assertEquals(emptyList(), memo.photos)
    }

    @Test
    fun `content가 2000자를 넘으면 불변식 위반`() {
        assertFailsWith<IllegalArgumentException> {
            FreeMemo.create("A1B2C3D4", "1234567890", "가".repeat(2001))
        }
    }

    @Test
    fun `content가 정확히 2000자면 통과`() {
        val memo = FreeMemo.create("A1B2C3D4", "1234567890", "가".repeat(2000))

        assertEquals(2000, memo.content!!.length)
    }

    @Test
    fun `withContent는 id와 photos를 유지한 채 content만 바꾼다`() {
        val original =
            FreeMemo.reconstitute(
                MemoId(7L),
                "A1B2C3D4",
                "1234567890",
                "old",
                listOf(MemoPhoto("memo/A1B2C3D4/a.jpg", 0)),
            )

        val updated = original.withContent("new")

        assertEquals("new", updated.content)
        assertEquals(MemoId(7L), updated.id)
        assertEquals(1, updated.photos.size)
    }

    @Test
    fun `withPhotos는 sortOrder로 정렬하고 6장이면 불변식 위반`() {
        val memo = FreeMemo.create("A1B2C3D4", "1234567890", null)

        val withTwo = memo.withPhotos(listOf(MemoPhoto("b", 1), MemoPhoto("a", 0)))
        assertEquals(listOf("a", "b"), withTwo.photos.map { it.objectKey })

        assertFailsWith<IllegalArgumentException> {
            memo.withPhotos((1..6).map { MemoPhoto("k$it", it) })
        }
    }
}
