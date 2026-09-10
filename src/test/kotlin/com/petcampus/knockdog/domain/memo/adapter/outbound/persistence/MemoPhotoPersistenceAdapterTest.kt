package com.petcampus.knockdog.domain.memo.adapter.outbound.persistence

import com.petcampus.knockdog.domain.memo.domain.MemoPhoto
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertNull

@SpringBootTest
@Transactional
class MemoPhotoPersistenceAdapterTest {
    @Autowired
    private lateinit var adapter: MemoPhotoPersistenceAdapter

    @Test
    fun `추가·조회·개수·삭제가 user_code와 target_id 단위로 동작한다`() {
        assertEquals(0, adapter.countByUserCodeAndTargetId("A1B2C3D4", "place-1"))

        val first = adapter.save(MemoPhoto.create("A1B2C3D4", "place-1", "memo/A1B2C3D4/a.webp", 0))
        adapter.save(MemoPhoto.create("A1B2C3D4", "place-1", "memo/A1B2C3D4/b.webp", 1))
        adapter.save(MemoPhoto.create("A1B2C3D4", "place-2", "memo/A1B2C3D4/c.webp", 0))

        assertEquals(2, adapter.countByUserCodeAndTargetId("A1B2C3D4", "place-1"))
        assertEquals(
            listOf("memo/A1B2C3D4/a.webp", "memo/A1B2C3D4/b.webp"),
            adapter.findAllByUserCodeAndTargetId("A1B2C3D4", "place-1").map { it.objectKey },
        )

        adapter.deleteById(requireNotNull(first.id))

        assertEquals(1, adapter.countByUserCodeAndTargetId("A1B2C3D4", "place-1"))
        assertNull(adapter.findById(requireNotNull(first.id)))
    }
}
