package com.petcampus.knockdog.domain.kindergarten.adapter.inbound.web

import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenBusinessHour
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenSource
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenStatus
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.assertEquals

class KindergartenSummaryResponseTest {
    private fun sample(businessHours: List<KindergartenBusinessHour> = emptyList()) =
        Kindergarten.reconstitute(
            id =
                com.petcampus.knockdog.domain.kindergarten.domain
                    .KindergartenId(1L),
            naverPlaceId = "12345",
            name = "테스트 유치원",
            phoneNumber = null,
            address = "서울시 중구 동호로12길",
            addressDetail = "2층",
            lat = 37.5,
            lng = 127.0,
            thumbnailS3Key = null,
            visitorReviewCount = 0,
            blogReviewCount = 0,
            source = KindergartenSource.CRAWLED,
            status = KindergartenStatus.ACTIVE,
            categories = emptyList(),
            businessHours = businessHours,
            links = emptyList(),
            options = emptyList(),
            priceImages = emptyList(),
            menus = emptyList(),
        )

    @Test
    fun `address와 addressDetail을 각자 담는다`() {
        val response = KindergartenSummaryResponse.from(sample(), userLat = 37.5, userLng = 127.0)

        assertEquals("서울시 중구 동호로12길", response.address)
        assertEquals("2층", response.addressDetail)
    }

    @Test
    fun `오늘이 휴무일이면 operationStatus는 CLOSED가 아니라 HOLIDAY다`() {
        val monday = LocalDateTime.of(2026, 9, 7, 14, 0)
        val offdayProfile =
            KindergartenBusinessHour(
                name = "DEFAULT",
                weekdayOpen = LocalTime.of(9, 0),
                weekdayClose = LocalTime.of(20, 0),
                weekendOpen = LocalTime.of(11, 0),
                weekendClose = LocalTime.of(18, 0),
                offdays = listOf(DayOfWeek.MONDAY),
            )

        val response = KindergartenSummaryResponse.from(sample(listOf(offdayProfile)), userLat = 37.5, userLng = 127.0, now = monday)

        assertEquals("HOLIDAY", response.operationStatus)
    }
}
