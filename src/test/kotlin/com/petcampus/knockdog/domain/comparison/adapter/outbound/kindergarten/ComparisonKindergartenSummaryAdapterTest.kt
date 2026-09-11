package com.petcampus.knockdog.domain.comparison.adapter.outbound.kindergarten

import com.petcampus.knockdog.domain.kindergarten.application.port.output.LoadKindergartenPort
import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenCategory
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenId
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenSource
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ComparisonKindergartenSummaryAdapterTest {
    private class StubLoadKindergartenPort(
        private val byId: Map<String, Kindergarten>,
    ) : LoadKindergartenPort {
        override fun findByNaverPlaceId(naverPlaceId: String) = byId[naverPlaceId]

        override fun findByNaverPlaceIds(naverPlaceIds: List<String>) = naverPlaceIds.mapNotNull { byId[it] }
    }

    private fun kindergarten(naverPlaceId: String) =
        Kindergarten.reconstitute(
            id = KindergartenId(1L),
            naverPlaceId = naverPlaceId,
            name = "유치원 $naverPlaceId",
            phoneNumber = null,
            address = "서울시 강남구",
            addressDetail = null,
            lat = 37.5,
            lng = 127.0,
            thumbnailS3Key = "thumb.webp",
            visitorReviewCount = 0,
            blogReviewCount = 0,
            source = KindergartenSource.CRAWLED,
            status = KindergartenStatus.ACTIVE,
            categories = listOf(KindergartenCategory("KINDERGARTEN")),
            businessHours = emptyList(),
            links = emptyList(),
            options = emptyList(),
            priceImages = emptyList(),
            menus = emptyList(),
        )

    @Test
    fun `빈 목록이면 조회하지 않고 빈 목록을 돌려준다`() {
        val adapter = ComparisonKindergartenSummaryAdapter(StubLoadKindergartenPort(emptyMap()))

        assertEquals(emptyList(), adapter.findByNaverPlaceIds(emptyList()))
    }

    @Test
    fun `존재하는 유치원만 요약으로 변환한다`() {
        val adapter =
            ComparisonKindergartenSummaryAdapter(StubLoadKindergartenPort(mapOf("n-1" to kindergarten("n-1"))))

        val summaries = adapter.findByNaverPlaceIds(listOf("n-1", "gone"))

        assertEquals(1, summaries.size)
        val summary = summaries.single()
        assertEquals("n-1", summary.id)
        assertEquals("유치원 n-1", summary.name)
        assertEquals("thumb.webp", summary.thumbnailS3Key)
        assertEquals(listOf("KINDERGARTEN"), summary.categories)
    }
}
