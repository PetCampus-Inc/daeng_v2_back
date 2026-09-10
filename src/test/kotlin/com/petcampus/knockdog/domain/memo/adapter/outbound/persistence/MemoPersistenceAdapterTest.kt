package com.petcampus.knockdog.domain.memo.adapter.outbound.persistence

import com.petcampus.knockdog.domain.memo.domain.Memo
import com.petcampus.knockdog.domain.memo.domain.MemoPhoto
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest
@Transactional
class MemoPersistenceAdapterTest {
    @Autowired
    private lateinit var adapter: MemoPersistenceAdapter

    @Test
    fun `없으면 null, 저장하면 조회된다`() {
        assertNull(adapter.findByUserCodeAndTargetId("A1B2C3D4", "place-1"))

        adapter.save(Memo.create("A1B2C3D4", "place-1", "첫 메모"))

        val found = adapter.findByUserCodeAndTargetId("A1B2C3D4", "place-1")
        assertNotNull(found)
        assertEquals("첫 메모", found.content)
    }

    @Test
    fun `같은 user_code, target_id로 다시 저장하면 새 row가 아니라 갱신된다`() {
        val first = adapter.save(Memo.create("A1B2C3D4", "place-1", "v1"))

        val updated = adapter.save(first.withContent("v2"))

        assertEquals(first.id, updated.id)
        assertEquals("v2", adapter.findByUserCodeAndTargetId("A1B2C3D4", "place-1")!!.content)
        assertEquals(1, adapter.findSummariesByUserCode("A1B2C3D4").size)
    }

    @Test
    fun `photos는 sort_order대로 저장·조회되고 재저장 시 전량 교체된다`() {
        val saved =
            adapter.save(
                Memo
                    .create("A1B2C3D4", "place-1", "m")
                    .withPhotos(listOf(MemoPhoto("memo/A1B2C3D4/b.jpg", 1), MemoPhoto("memo/A1B2C3D4/a.jpg", 0))),
            )
        assertEquals(
            listOf("memo/A1B2C3D4/a.jpg", "memo/A1B2C3D4/b.jpg"),
            adapter.findByUserCodeAndTargetId("A1B2C3D4", "place-1")!!.photos.map { it.objectKey },
        )

        adapter.save(saved.withPhotos(listOf(MemoPhoto("memo/A1B2C3D4/c.jpg", 0))))
        assertEquals(
            listOf("memo/A1B2C3D4/c.jpg"),
            adapter.findByUserCodeAndTargetId("A1B2C3D4", "place-1")!!.photos.map { it.objectKey },
        )
    }

    @Test
    fun `findSummariesByUserCode는 내 메모만 updated_at 내림차순`() {
        adapter.save(Memo.create("A1B2C3D4", "place-1", "a"))
        adapter.save(Memo.create("A1B2C3D4", "place-2", "b"))
        adapter.save(Memo.create("OTHER999", "place-3", "c"))

        val mine = adapter.findSummariesByUserCode("A1B2C3D4")

        assertEquals(listOf("place-2", "place-1"), mine.map { it.targetId })
        assertNotNull(mine.first().memoDate)
    }
}
