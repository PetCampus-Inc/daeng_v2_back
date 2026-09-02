package com.petcampus.knockdog.domain.kindergarten.adapter.outbound.seed

import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenOptionGroup
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KindergartenSeedConverterTest {
    private fun sample(
        id: Long = 1253111667,
        businessHours: List<CrawledBusinessHour> = emptyList(),
    ) = CrawledKindergarten(
        id = id,
        name = "디어코코 강아지유치원",
        categories = listOf("KINDERGARTEN", "HOTEL"),
        tel = "",
        thumbnailS3Key = "thumb.png",
        menuImageS3Keys = listOf("menu0.png", "menu1.png"),
        roadAddress = "서울시 중구 동호로12길",
        lat = 37.5,
        lng = 127.0,
        links = listOf(CrawledLink("INSTAGRAM", "http://instagram.com/x")),
        dogBreedsAccepted = listOf("ALL_BREEDS"),
        dogServices = listOf("DAYCARE", "HOTEL"),
        dogSafetyFacilities = emptyList(),
        visitorAmenities = emptyList(),
        businessHours = businessHours,
        reviewCount = CrawledReviewCount(blogReviewCount = 135, visitReviewCount = 103),
    )

    @Test
    fun `크롤링 원본 id를 문자열 naverPlaceId로 변환한다`() {
        val kindergarten = KindergartenSeedConverter.toDomain(sample(id = 1253111667), emptyList())

        assertEquals("1253111667", kindergarten.naverPlaceId)
    }

    @Test
    fun `전화번호가 빈 문자열이면 null로 정규화한다`() {
        val kindergarten = KindergartenSeedConverter.toDomain(sample(), emptyList())

        assertNull(kindergarten.phoneNumber)
    }

    @Test
    fun `썸네일과 메뉴이미지 순서가 유지된 채로 가격표 이미지가 된다`() {
        val kindergarten = KindergartenSeedConverter.toDomain(sample(), emptyList())

        assertEquals(listOf("menu0.png", "menu1.png"), kindergarten.priceImages.sortedBy { it.displayOrder }.map { it.s3Key })
    }

    @Test
    fun `견종수용 배열은 DOG_BREED 그룹 옵션이 된다`() {
        val kindergarten = KindergartenSeedConverter.toDomain(sample(), emptyList())

        assertEquals(listOf("ALL_BREEDS"), kindergarten.optionsOf(KindergartenOptionGroup.DOG_BREED).map { it.code })
        assertEquals(listOf("DAYCARE", "HOTEL"), kindergarten.optionsOf(KindergartenOptionGroup.DOG_SERVICE).map { it.code }.sorted())
    }

    @Test
    fun `영업시간 offdays 문자열을 DayOfWeek로 변환한다`() {
        val businessHours =
            listOf(
                CrawledBusinessHour(
                    name = "DEFAULT",
                    weekdays = CrawledTimeRange("08:00", "21:00"),
                    weekends = CrawledTimeRange("11:00", "21:00"),
                    offdays = listOf("THURSDAY"),
                ),
            )

        val kindergarten = KindergartenSeedConverter.toDomain(sample(businessHours = businessHours), emptyList())

        val hour = kindergarten.businessHours.single()
        assertEquals(LocalTime.of(8, 0), hour.weekdayOpen)
        assertEquals(listOf(DayOfWeek.THURSDAY), hour.offdays)
    }

    @Test
    fun `메뉴는 원본 순서를 displayOrder로 보존한다`() {
        val menus =
            listOf(
                CrawledMenu(
                    kindergartenId = 1L,
                    productType = "COUNT_TICKET",
                    serviceType = "DAYCARE",
                    productName = "반일반",
                    price = 30000.0,
                ),
                CrawledMenu(
                    kindergartenId = 1L,
                    productType = "MONTHLY_TICKET",
                    serviceType = "DAYCARE",
                    productName = "월정액",
                    price = 300000.0,
                ),
            )

        val kindergarten = KindergartenSeedConverter.toDomain(sample(), menus)

        assertEquals(listOf(0, 1), kindergarten.menus.map { it.displayOrder })
        assertTrue(kindergarten.menus.any { it.productName == "월정액" && it.price == 300000 })
    }
}
