package com.petcampus.knockdog.domain.kindergarten.adapter.outbound.persistence

import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenCategory
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenMenu
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@Transactional
class KindergartenPersistenceAdapterCardSummaryTest {
    @Autowired
    private lateinit var adapter: KindergartenPersistenceAdapter

    @Autowired
    private lateinit var kindergartenJpaRepository: KindergartenJpaRepository

    private fun menu(price: Int?) =
        KindergartenMenu(
            productType = "COUNT_TICKET",
            serviceType = "DAYCARE",
            productName = "이용권",
            unit = null,
            unitLabel = null,
            unitType = null,
            weightRange = null,
            price = price,
            hourlyPrice = null,
            isMinPrice = false,
            isMaxPrice = false,
            totalDurationLabel = null,
            totalDurationMinutes = null,
            displayOrder = 0,
        )

    private fun seed(
        naverPlaceId: String,
        categories: List<String>,
        menus: List<KindergartenMenu>,
        blogReviewCount: Int = 0,
    ) = adapter.save(
        Kindergarten.seedFromCrawl(
            naverPlaceId = naverPlaceId,
            name = "유치원 $naverPlaceId",
            phoneNumber = null,
            address = "서울 강남구",
            addressDetail = null,
            lat = 37.5,
            lng = 127.0,
            thumbnailS3Key = "thumbnail/$naverPlaceId.webp",
            visitorReviewCount = 7,
            blogReviewCount = blogReviewCount,
            categories = categories.map { KindergartenCategory(it) },
            businessHours = emptyList(),
            links = emptyList(),
            options = emptyList(),
            priceImages = emptyList(),
            menus = menus,
        ),
    )

    @Test
    fun `카드 요약은 카테고리와 최저 요금을 유치원별로 채운다`() {
        seed("card-a", listOf("KINDERGARTEN", "HOTEL"), listOf(menu(30000), menu(20000), menu(null)), blogReviewCount = 128)
        seed("card-b", listOf("KINDERGARTEN"), emptyList())

        val summaries = adapter.findCardSummariesByNaverPlaceIds(listOf("card-a", "card-b", "card-missing")).associateBy { it.id }

        assertEquals(setOf("card-a", "card-b"), summaries.keys)
        val first = summaries.getValue("card-a")
        assertEquals(setOf("KINDERGARTEN", "HOTEL"), first.categories.toSet())
        assertEquals(20000, first.lowestPrice)
        assertEquals(128, first.blogReviewCount)
        assertEquals("서울 강남구", first.address)
        assertFalse(first.closed)
        val second = summaries.getValue("card-b")
        assertEquals(listOf("KINDERGARTEN"), second.categories)
        assertEquals(0, second.lowestPrice)
    }

    @Test
    fun `폐업 유치원은 closed로 표시한다`() {
        kindergartenJpaRepository.save(
            KindergartenJpaEntity(
                naverPlaceId = "card-closed",
                name = "폐업 유치원",
                phoneNumber = null,
                address = "서울 강남구",
                addressDetail = null,
                lat = null,
                lng = null,
                thumbnailS3Key = null,
                visitorReviewCount = 0,
                blogReviewCount = 0,
                source = "CRAWLED",
                status = "CLOSED",
            ),
        )

        val summary = adapter.findCardSummariesByNaverPlaceIds(listOf("card-closed")).single()

        assertTrue(summary.closed)
    }

    @Test
    fun `빈 목록은 조회 없이 빈 결과를 돌려준다`() {
        assertEquals(emptyList(), adapter.findCardSummariesByNaverPlaceIds(emptyList()))
    }
}
