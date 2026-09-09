package com.petcampus.knockdog.domain.memo.adapter.outbound.persistence

import com.petcampus.knockdog.domain.memo.domain.ChecklistSubmission
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertNull

@SpringBootTest
@Transactional
class ChecklistSubmissionPersistenceAdapterTest {
    @Autowired
    private lateinit var adapter: ChecklistSubmissionPersistenceAdapter

    @Test
    fun `없으면 null, 저장하면 answers까지 복원`() {
        assertNull(adapter.findByUserCodeAndTargetId("A1B2C3D4", "place-1"))

        adapter.save(
            ChecklistSubmission.create(
                "A1B2C3D4",
                "place-1",
                "1",
                mapOf("q_vaccine_proof_required" to "YES", "q_max_dogs_per_day" to "30"),
            ),
        )

        val found = adapter.findByUserCodeAndTargetId("A1B2C3D4", "place-1")!!
        assertEquals(mapOf("q_vaccine_proof_required" to "YES", "q_max_dogs_per_day" to "30"), found.answers)
    }

    @Test
    fun `재저장은 새 row가 아니라 answers 교체`() {
        val first = adapter.save(ChecklistSubmission.create("A1B2C3D4", "p", "1", mapOf("q1" to "YES")))

        adapter.save(first.withAnswers("1", mapOf("q1" to "NO", "q2" to "YES")))

        val found = adapter.findByUserCodeAndTargetId("A1B2C3D4", "p")!!
        assertEquals(first.id, found.id)
        assertEquals(mapOf("q1" to "NO", "q2" to "YES"), found.answers)
    }
}
