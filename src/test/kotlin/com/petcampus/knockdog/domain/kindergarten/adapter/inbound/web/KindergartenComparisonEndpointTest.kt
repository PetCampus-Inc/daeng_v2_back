package com.petcampus.knockdog.domain.kindergarten.adapter.inbound.web

import com.petcampus.knockdog.domain.auth.application.port.output.TokenPort
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.comparison.adapter.outbound.persistence.ComparisonHistoryJpaRepository
import com.petcampus.knockdog.domain.kindergarten.application.port.output.LoadComparisonAddressesPort
import com.petcampus.knockdog.domain.kindergarten.application.port.output.LoadKindergartenPort
import com.petcampus.knockdog.domain.kindergarten.domain.ComparisonReferencePoint
import com.petcampus.knockdog.domain.kindergarten.domain.ComparisonReferencePointType
import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenBusinessHour
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenId
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenSource
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.test.assertEquals

@SpringBootTest
@AutoConfigureMockMvc
@Import(KindergartenComparisonEndpointTest.FakePortsConfig::class)
class KindergartenComparisonEndpointTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var tokenPort: TokenPort

    @Autowired
    private lateinit var comparisonHistoryJpaRepository: ComparisonHistoryJpaRepository

    @Test
    fun `인증 없이 두 유치원을 비교할 수 있다`() {
        mockMvc
            .perform(get("/api/v1/kindergartens/comparisons").param("ids", "A").param("ids", "B"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].id").value("A"))
            .andExpect(jsonPath("$.data[0].service").isArray)
            .andExpect(jsonPath("$.data[0].distance").isEmpty)
    }

    @Test
    fun `operatingSchedule은 weekday-weekend를 open-close 객체로 내려준다`() {
        mockMvc
            .perform(get("/api/v1/kindergartens/comparisons").param("ids", "A").param("ids", "B"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].operatingSchedule.weekday.open").value("09:00"))
            .andExpect(jsonPath("$.data[0].operatingSchedule.weekday.close").value("20:00"))
            .andExpect(jsonPath("$.data[0].operatingSchedule.closedDays[0]").value("SUNDAY"))
    }

    @Test
    fun `ids가 2개가 아니면 400 COMPARISON_TARGET_COUNT다`() {
        mockMvc
            .perform(get("/api/v1/kindergartens/comparisons").param("ids", "A"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("COMPARISON_TARGET_COUNT"))
    }

    @Test
    fun `없는 유치원이면 404 RESOURCE_NOT_FOUND다`() {
        mockMvc
            .perform(get("/api/v1/kindergartens/comparisons").param("ids", "A").param("ids", "MISSING"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
    }

    @Test
    fun `lat lng를 주면 distance 기준점이 OTHER 하나다`() {
        mockMvc
            .perform(
                get("/api/v1/kindergartens/comparisons")
                    .param("ids", "A")
                    .param("ids", "B")
                    .param("lat", "37.4979")
                    .param("lng", "127.0276"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].distance.length()").value(1))
            .andExpect(jsonPath("$.data[0].distance[0].referencePoint").value("OTHER"))
            .andExpect(jsonPath("$.data[0].distance[0].transitTimes").isEmpty)
    }

    @Test
    fun `로그인하고 위치를 안 주면 저장 주소가 distance 기준점이 된다`() {
        val bearer = "Bearer " + tokenPort.issueAccessToken(UserCode("A1B2C3D4"))

        mockMvc
            .perform(
                get("/api/v1/kindergartens/comparisons")
                    .header("Authorization", bearer)
                    .param("ids", "A")
                    .param("ids", "B"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].distance.length()").value(1))
            .andExpect(jsonPath("$.data[0].distance[0].referencePoint").value("HOME"))
    }

    @Test
    fun `로그인 상태로 비교하면 비교 히스토리가 기록된다`() {
        comparisonHistoryJpaRepository.deleteAll()
        val bearer = "Bearer " + tokenPort.issueAccessToken(UserCode("H1I2J3K4"))

        mockMvc
            .perform(
                get("/api/v1/kindergartens/comparisons")
                    .header("Authorization", bearer)
                    .param("ids", "A")
                    .param("ids", "B"),
            ).andExpect(status().isOk)

        val recorded = comparisonHistoryJpaRepository.findAll().single { it.userCode == "H1I2J3K4" }
        assertEquals("A", recorded.kindergartenIdA)
        assertEquals("B", recorded.kindergartenIdB)
    }

    @Test
    fun `비로그인으로 비교하면 히스토리를 기록하지 않는다`() {
        comparisonHistoryJpaRepository.deleteAll()

        mockMvc
            .perform(get("/api/v1/kindergartens/comparisons").param("ids", "A").param("ids", "B"))
            .andExpect(status().isOk)

        assertEquals(0, comparisonHistoryJpaRepository.count())
    }

    @TestConfiguration
    class FakePortsConfig {
        private fun kindergarten(naverPlaceId: String) =
            Kindergarten.reconstitute(
                id = KindergartenId(naverPlaceId.hashCode().toLong()),
                naverPlaceId = naverPlaceId,
                name = "유치원 $naverPlaceId",
                phoneNumber = null,
                address = "서울시 강남구",
                addressDetail = null,
                lat = 37.5,
                lng = 127.0,
                thumbnailS3Key = null,
                visitorReviewCount = 0,
                blogReviewCount = 0,
                source = KindergartenSource.CRAWLED,
                status = KindergartenStatus.ACTIVE,
                categories = emptyList(),
                businessHours =
                    listOf(
                        KindergartenBusinessHour(
                            name = "DEFAULT",
                            weekdayOpen = LocalTime.of(9, 0),
                            weekdayClose = LocalTime.of(20, 0),
                            weekendOpen = LocalTime.of(10, 0),
                            weekendClose = LocalTime.of(18, 0),
                            offdays = listOf(DayOfWeek.SUNDAY),
                        ),
                    ),
                links = emptyList(),
                options = emptyList(),
                priceImages = emptyList(),
                menus = emptyList(),
            )

        @Bean
        @Primary
        fun fakeLoadKindergartenPort(): LoadKindergartenPort =
            object : LoadKindergartenPort {
                private val byId = listOf("A", "B").associateWith { kindergarten(it) }

                override fun findByNaverPlaceId(naverPlaceId: String) = byId[naverPlaceId]

                override fun findByNaverPlaceIds(naverPlaceIds: List<String>) = naverPlaceIds.mapNotNull { byId[it] }
            }

        @Bean
        @Primary
        fun fakeLoadComparisonAddressesPort(): LoadComparisonAddressesPort =
            object : LoadComparisonAddressesPort {
                override fun findByUserCode(userCode: String) =
                    listOf(ComparisonReferencePoint(ComparisonReferencePointType.HOME, 37.6, 127.1))
            }
    }
}
