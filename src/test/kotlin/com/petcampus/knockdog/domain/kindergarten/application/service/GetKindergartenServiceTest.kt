package com.petcampus.knockdog.domain.kindergarten.application.service

import com.petcampus.knockdog.domain.kindergarten.application.port.output.LoadKindergartenPort
import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenId
import com.petcampus.knockdog.global.exception.BusinessException
import com.petcampus.knockdog.global.exception.CommonErrorCode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetKindergartenServiceTest {
    private class FixedLoadPort(
        private val kindergarten: Kindergarten?,
    ) : LoadKindergartenPort {
        override fun findByNaverPlaceId(naverPlaceId: String) = kindergarten
    }

    private fun sampleKindergarten(id: Long = 1L) =
        Kindergarten.reconstitute(
            id = KindergartenId(id),
            naverPlaceId = "12345",
            name = "테스트 유치원",
            phoneNumber = null,
            address = "서울시 강남구",
            addressDetail = null,
            lat = 37.5,
            lng = 127.0,
            thumbnailS3Key = null,
            visitorReviewCount = 0,
            blogReviewCount = 0,
            source = com.petcampus.knockdog.domain.kindergarten.domain.KindergartenSource.CRAWLED,
            status = com.petcampus.knockdog.domain.kindergarten.domain.KindergartenStatus.ACTIVE,
            categories = emptyList(),
            businessHours = emptyList(),
            links = emptyList(),
            options = emptyList(),
            priceImages = emptyList(),
            menus = emptyList(),
        )

    @Test
    fun `존재하는 naverPlaceId면 유치원을 반환한다`() {
        val kindergarten = sampleKindergarten()
        val service = GetKindergartenService(FixedLoadPort(kindergarten))

        val result = service.getByNaverPlaceId("12345")

        assertEquals("테스트 유치원", result.name)
    }

    @Test
    fun `존재하지 않는 naverPlaceId면 RESOURCE_NOT_FOUND 예외를 던진다`() {
        val service = GetKindergartenService(FixedLoadPort(null))

        val exception =
            assertFailsWith<BusinessException> {
                service.getByNaverPlaceId("no-such-id")
            }
        assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND, exception.errorCode)
    }
}
