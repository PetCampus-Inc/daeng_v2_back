package com.petcampus.knockdog.domain.comparison.adapter.inbound.web

import com.petcampus.knockdog.domain.auth.application.port.output.TokenPort
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.comparison.adapter.outbound.persistence.ComparisonHistoryJpaEntity
import com.petcampus.knockdog.domain.comparison.adapter.outbound.persistence.ComparisonHistoryJpaRepository
import com.petcampus.knockdog.domain.comparison.application.port.output.ComparisonKindergartenSummary
import com.petcampus.knockdog.domain.comparison.application.port.output.LoadComparisonKindergartenSummariesPort
import com.petcampus.knockdog.domain.comparison.application.port.output.SaveComparisonHistoryPort
import com.petcampus.knockdog.domain.comparison.domain.ComparisonHistory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
@Import(ComparisonHistoryEndpointTest.FakeSummariesConfig::class)
class ComparisonHistoryEndpointTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var tokenPort: TokenPort

    @Autowired
    private lateinit var repository: ComparisonHistoryJpaRepository

    @Autowired
    private lateinit var saveComparisonHistoryPort: SaveComparisonHistoryPort

    private val ownerCode = "A1B2C3D4"

    private fun bearer(userCode: String = ownerCode) = "Bearer " + tokenPort.issueAccessToken(UserCode(userCode))

    @BeforeEach
    fun clean() {
        repository.deleteAll()
    }

    private fun seed(
        userCode: String,
        a: String,
        b: String,
    ): Long =
        repository
            .save(ComparisonHistoryJpaEntity(userCode = userCode, kindergartenIdA = minOf(a, b), kindergartenIdB = maxOf(a, b)))
            .id!!

    @Test
    fun `인증 없이 히스토리를 조회하면 401이다`() {
        mockMvc.perform(get("/api/v1/kindergartens/comparisons/history")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `히스토리를 최근순으로 조회한다`() {
        seed(ownerCode, "n-1", "n-2")

        mockMvc
            .perform(get("/api/v1/kindergartens/comparisons/history").header("Authorization", bearer()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].kindergartens.length()").value(2))
            .andExpect(jsonPath("$.data[0].kindergartens[0].name").value("유치원 n-1"))
    }

    @Test
    fun `본인 히스토리를 삭제하면 조회 목록에서 사라진다`() {
        val id = seed(ownerCode, "n-1", "n-2")

        mockMvc
            .perform(delete("/api/v1/kindergartens/comparisons/history/$id").header("Authorization", bearer()))
            .andExpect(status().isOk)

        mockMvc
            .perform(get("/api/v1/kindergartens/comparisons/history").header("Authorization", bearer()))
            .andExpect(jsonPath("$.data.length()").value(0))
    }

    @Test
    fun `남의 히스토리를 삭제하면 403이다`() {
        val id = seed("OTHER123", "n-1", "n-2")

        mockMvc
            .perform(delete("/api/v1/kindergartens/comparisons/history/$id").header("Authorization", bearer()))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("COMPARISON_HISTORY_NOT_OWNER"))
    }

    @Test
    fun `같은 두 유치원을 순서만 바꿔 다시 비교하면 새 행이 생기지 않고 갱신된다`() {
        saveComparisonHistoryPort.upsert(ComparisonHistory.create(ownerCode, "n-b", "n-a"))
        val first = repository.findAll().single()

        saveComparisonHistoryPort.upsert(ComparisonHistory.create(ownerCode, "n-a", "n-b"))

        val rows = repository.findAll()
        assertEquals(1, rows.size)
        assertEquals(first.id, rows.single().id)
        assertTrue(!rows.single().updatedAt.isBefore(first.updatedAt))
    }

    @Test
    fun `soft delete된 이력을 다시 비교하면 되살아난다`() {
        val id = seed(ownerCode, "n-1", "n-2")
        saveComparisonHistoryPort.softDeleteById(id)

        saveComparisonHistoryPort.upsert(ComparisonHistory.create(ownerCode, "n-1", "n-2"))

        mockMvc
            .perform(get("/api/v1/kindergartens/comparisons/history").header("Authorization", bearer()))
            .andExpect(jsonPath("$.data.length()").value(1))
    }

    @Test
    fun `없는 히스토리를 삭제하면 404다`() {
        mockMvc
            .perform(delete("/api/v1/kindergartens/comparisons/history/99999").header("Authorization", bearer()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("COMPARISON_HISTORY_NOT_FOUND"))
    }

    @TestConfiguration
    class FakeSummariesConfig {
        @Bean
        @Primary
        fun fakeSummariesPort(): LoadComparisonKindergartenSummariesPort =
            object : LoadComparisonKindergartenSummariesPort {
                override fun findByNaverPlaceIds(naverPlaceIds: List<String>) =
                    naverPlaceIds.map { ComparisonKindergartenSummary(it, "유치원 $it", null, listOf("KINDERGARTEN")) }
            }
    }
}
