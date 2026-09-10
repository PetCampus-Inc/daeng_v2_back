package com.petcampus.knockdog.domain.kindergarten.adapter.inbound.web

import com.petcampus.knockdog.domain.kindergarten.domain.ComparisonReferencePoint
import com.petcampus.knockdog.domain.kindergarten.domain.ComparisonReferencePointType
import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenBusinessHour
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenCategory
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenId
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenMenu
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenSource
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenStatus
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KindergartenComparisonResponseTest {
    private fun menu(
        productType: String,
        serviceType: String,
        productName: String,
        price: Int?,
        hourlyPrice: Int?,
    ) = KindergartenMenu(
        productType = productType,
        serviceType = serviceType,
        productName = productName,
        unit = null,
        unitLabel = null,
        unitType = null,
        weightRange = null,
        price = price,
        hourlyPrice = hourlyPrice,
        isMinPrice = false,
        isMaxPrice = false,
        totalDurationLabel = null,
        totalDurationMinutes = null,
        displayOrder = 0,
    )

    private fun kindergarten(
        lat: Double? = 37.5,
        lng: Double? = 127.0,
        businessHours: List<KindergartenBusinessHour> = emptyList(),
        menus: List<KindergartenMenu> = emptyList(),
    ) = Kindergarten.reconstitute(
        id = KindergartenId(1L),
        naverPlaceId = "12345",
        name = "테스트 유치원",
        phoneNumber = null,
        address = "서울시 강남구",
        addressDetail = null,
        lat = lat,
        lng = lng,
        thumbnailS3Key = "thumb.webp",
        visitorReviewCount = 0,
        blogReviewCount = 0,
        source = KindergartenSource.CRAWLED,
        status = KindergartenStatus.ACTIVE,
        categories = listOf(KindergartenCategory("KINDERGARTEN")),
        businessHours = businessHours,
        links = emptyList(),
        options = emptyList(),
        priceImages = emptyList(),
        menus = menus,
    )

    @Test
    fun `메뉴가 없으면 pricing은 null이다`() {
        val response = KindergartenComparisonResponse.from(kindergarten(), emptyList())

        assertNull(response.pricing)
    }

    @Test
    fun `pricing은 서비스종류별 최저-최고와 정책 평균을 담는다`() {
        val menus =
            listOf(
                menu("COUNT_TICKET", "DAYCARE", "산책", price = 10000, hourlyPrice = 5000),
                menu("COUNT_TICKET", "DAYCARE", "종일반", price = 50000, hourlyPrice = 7000),
            )

        val pricing = KindergartenComparisonResponse.from(kindergarten(menus = menus), emptyList()).pricing!!

        assertEquals(6000, pricing.countHourlyAvg)
        val daycare = pricing.products.single()
        assertEquals("산책", daycare.min.name)
        assertEquals(10000, daycare.min.price)
        assertEquals("종일반", daycare.max.name)
        assertEquals(6000, daycare.countTicketAvg)
    }

    @Test
    fun `operatingSchedule은 DEFAULT 프로필을 우선한다`() {
        val hotel =
            KindergartenBusinessHour("HOTEL", LocalTime.of(0, 0), LocalTime.of(23, 59), null, null, emptyList())
        val default =
            KindergartenBusinessHour(
                "DEFAULT",
                LocalTime.of(9, 0),
                LocalTime.of(20, 0),
                LocalTime.of(10, 0),
                LocalTime.of(18, 0),
                listOf(DayOfWeek.SUNDAY),
            )

        val schedule =
            KindergartenComparisonResponse
                .from(
                    kindergarten(businessHours = listOf(hotel, default)),
                    emptyList(),
                ).operatingSchedule!!

        assertEquals(KindergartenComparisonResponse.TimeRange(LocalTime.of(9, 0), LocalTime.of(20, 0)), schedule.weekday)
        assertEquals(KindergartenComparisonResponse.TimeRange(LocalTime.of(10, 0), LocalTime.of(18, 0)), schedule.weekend)
        assertEquals(listOf("SUNDAY"), schedule.closedDays)
    }

    @Test
    fun `영업시간이 비어 있으면 weekday는 null이다`() {
        val profile = KindergartenBusinessHour("DEFAULT", null, null, null, null, listOf(DayOfWeek.MONDAY))

        val schedule = KindergartenComparisonResponse.from(kindergarten(businessHours = listOf(profile)), emptyList()).operatingSchedule!!

        assertNull(schedule.weekday)
        assertNull(schedule.weekend)
        assertEquals(listOf("MONDAY"), schedule.closedDays)
    }

    @Test
    fun `distance는 기준점마다 직선거리를 담고 transitTimes는 비어 있다`() {
        val points =
            listOf(
                ComparisonReferencePoint(ComparisonReferencePointType.HOME, 37.5, 127.0),
                ComparisonReferencePoint(ComparisonReferencePointType.OTHER, 37.4979, 127.0276),
            )

        val distance = KindergartenComparisonResponse.from(kindergarten(lat = 37.5663, lng = 126.9779), points).distance

        assertEquals(listOf("HOME", "OTHER"), distance.map { it.referencePoint })
        assertEquals(listOf("7.6km", "8.8km"), distance.map { it.distance })
        assertEquals(emptyList(), distance.first().transitTimes)
    }

    @Test
    fun `좌표가 없는 유치원은 distance가 비어 있다`() {
        val points = listOf(ComparisonReferencePoint(ComparisonReferencePointType.HOME, 37.5, 127.0))

        val distance = KindergartenComparisonResponse.from(kindergarten(lat = null, lng = null), points).distance

        assertEquals(emptyList(), distance)
    }
}
