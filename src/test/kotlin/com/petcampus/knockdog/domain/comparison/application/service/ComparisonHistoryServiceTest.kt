package com.petcampus.knockdog.domain.comparison.application.service

import com.petcampus.knockdog.domain.comparison.application.ComparisonHistoryErrorCode
import com.petcampus.knockdog.domain.comparison.application.port.output.ComparisonKindergartenSummary
import com.petcampus.knockdog.domain.comparison.application.port.output.LoadComparisonHistoryPort
import com.petcampus.knockdog.domain.comparison.application.port.output.LoadComparisonKindergartenSummariesPort
import com.petcampus.knockdog.domain.comparison.application.port.output.SaveComparisonHistoryPort
import com.petcampus.knockdog.domain.comparison.domain.ComparisonHistory
import com.petcampus.knockdog.domain.comparison.domain.ComparisonHistoryId
import com.petcampus.knockdog.global.exception.BusinessException
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ComparisonHistoryServiceTest {
    private class RecordingSavePort : SaveComparisonHistoryPort {
        val upserted = mutableListOf<Triple<String, String, String>>()
        val softDeleted = mutableListOf<Long>()

        override fun upsert(history: ComparisonHistory) {
            upserted += Triple(history.userCode, history.kindergartenIdA, history.kindergartenIdB)
        }

        override fun softDeleteById(id: Long) {
            softDeleted += id
        }
    }

    private class StubLoadPort(
        private val recent: List<ComparisonHistory> = emptyList(),
        private val byId: Map<Long, ComparisonHistory> = emptyMap(),
    ) : LoadComparisonHistoryPort {
        var requestedLimit: Int = -1

        override fun findRecentByUserCode(
            userCode: String,
            limit: Int,
        ): List<ComparisonHistory> {
            requestedLimit = limit
            return recent
        }

        override fun findById(id: Long) = byId[id]
    }

    private class StubSummariesPort(
        private val summaries: List<ComparisonKindergartenSummary>,
    ) : LoadComparisonKindergartenSummariesPort {
        override fun findByNaverPlaceIds(naverPlaceIds: List<String>) = summaries.filter { it.id in naverPlaceIds }
    }

    private fun history(
        id: Long,
        userCode: String = "A1B2C3D4",
        first: String = "n-1",
        second: String = "n-2",
    ) = ComparisonHistory.reconstitute(
        id = ComparisonHistoryId(id),
        userCode = userCode,
        kindergartenIdA = minOf(first, second),
        kindergartenIdB = maxOf(first, second),
        comparedAt = LocalDateTime.of(2026, 9, 11, 10, 0, 0),
    )

    private fun summary(id: String) = ComparisonKindergartenSummary(id, "유치원 $id", null, listOf("KINDERGARTEN"))

    @Test
    fun `저장하면 두 ID를 정렬해 upsert한다`() {
        val savePort = RecordingSavePort()
        val service = ComparisonHistoryService(savePort, StubLoadPort(), StubSummariesPort(emptyList()))

        service.save("A1B2C3D4", listOf("n-9", "n-2"))

        assertEquals(listOf(Triple("A1B2C3D4", "n-2", "n-9")), savePort.upserted)
    }

    @Test
    fun `유치원이 2곳이 아니면 저장하지 않는다`() {
        val service = ComparisonHistoryService(RecordingSavePort(), StubLoadPort(), StubSummariesPort(emptyList()))

        assertFailsWith<IllegalArgumentException> { service.save("A1B2C3D4", listOf("n-1")) }
    }

    @Test
    fun `조회는 limit을 1에서 50 사이로 조인다`() {
        val loadPort = StubLoadPort()
        val service = ComparisonHistoryService(RecordingSavePort(), loadPort, StubSummariesPort(emptyList()))

        service.list("A1B2C3D4", 0)
        assertEquals(10, loadPort.requestedLimit)

        service.list("A1B2C3D4", 999)
        assertEquals(50, loadPort.requestedLimit)
    }

    @Test
    fun `조회는 최근 히스토리마다 유치원 요약을 채운다`() {
        val loadPort = StubLoadPort(recent = listOf(history(1, first = "n-1", second = "n-2")))
        val service =
            ComparisonHistoryService(
                RecordingSavePort(),
                loadPort,
                StubSummariesPort(listOf(summary("n-1"), summary("n-2"))),
            )

        val result = service.list("A1B2C3D4", 10)

        assertEquals(1L, result.single().id)
        assertEquals(listOf("n-1", "n-2"), result.single().kindergartens.map { it.id })
    }

    @Test
    fun `없어진 유치원은 요약에서 빠지고 히스토리는 남는다`() {
        val loadPort = StubLoadPort(recent = listOf(history(1, first = "n-1", second = "gone")))
        val service =
            ComparisonHistoryService(RecordingSavePort(), loadPort, StubSummariesPort(listOf(summary("n-1"))))

        val result = service.list("A1B2C3D4", 10)

        assertEquals(listOf("n-1"), result.single().kindergartens.map { it.id })
    }

    @Test
    fun `삭제 대상이 없으면 NOT_FOUND다`() {
        val service = ComparisonHistoryService(RecordingSavePort(), StubLoadPort(), StubSummariesPort(emptyList()))

        val exception = assertFailsWith<BusinessException> { service.delete("A1B2C3D4", 1L) }
        assertEquals(ComparisonHistoryErrorCode.NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `본인 히스토리가 아니면 NOT_OWNER다`() {
        val loadPort = StubLoadPort(byId = mapOf(1L to history(1, userCode = "OTHER123")))
        val service = ComparisonHistoryService(RecordingSavePort(), loadPort, StubSummariesPort(emptyList()))

        val exception = assertFailsWith<BusinessException> { service.delete("A1B2C3D4", 1L) }
        assertEquals(ComparisonHistoryErrorCode.NOT_OWNER, exception.errorCode)
    }

    @Test
    fun `본인 히스토리면 soft delete한다`() {
        val savePort = RecordingSavePort()
        val loadPort = StubLoadPort(byId = mapOf(1L to history(1, userCode = "A1B2C3D4")))
        val service = ComparisonHistoryService(savePort, loadPort, StubSummariesPort(emptyList()))

        service.delete("A1B2C3D4", 1L)

        assertTrue(1L in savePort.softDeleted)
    }
}
