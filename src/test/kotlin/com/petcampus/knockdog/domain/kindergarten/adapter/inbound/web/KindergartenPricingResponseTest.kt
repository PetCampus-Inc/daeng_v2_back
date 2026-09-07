package com.petcampus.knockdog.domain.kindergarten.adapter.inbound.web

import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenId
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenMenu
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenSource
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class KindergartenPricingResponseTest {
    private fun menu(
        productName: String,
        serviceType: String,
        displayOrder: Int,
    ) = KindergartenMenu(
        productType = "COUNT_TICKET",
        serviceType = serviceType,
        productName = productName,
        unit = null,
        unitLabel = null,
        unitType = null,
        weightRange = null,
        price = 10000,
        hourlyPrice = null,
        isMinPrice = false,
        isMaxPrice = false,
        totalDurationLabel = null,
        totalDurationMinutes = null,
        displayOrder = displayOrder,
    )

    private fun sample(menus: List<KindergartenMenu>) =
        Kindergarten.reconstitute(
            id = KindergartenId(1L),
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
            source = KindergartenSource.CRAWLED,
            status = KindergartenStatus.ACTIVE,
            categories = emptyList(),
            businessHours = emptyList(),
            links = emptyList(),
            options = emptyList(),
            priceImages = emptyList(),
            menus = menus,
        )

    @Test
    fun `같은 상품명이라도 서비스 유형이 다르면 다른 카테고리로 분리한다`() {
        val menus =
            listOf(
                menu(productName = "1:1 도그 피트니스", serviceType = "DAYCARE", displayOrder = 0),
                menu(productName = "1:1 도그 피트니스", serviceType = "EXPERIENCE_TICKET", displayOrder = 1),
            )

        val response = KindergartenPricingResponse.from(sample(menus))

        assertEquals(2, response.productCategories.size)
        assertEquals(setOf("DAYCARE", "EXPERIENCE_TICKET"), response.productCategories.map { it.serviceType }.toSet())
    }

    @Test
    fun `같은 상품명·같은 서비스 유형이면 한 카테고리로 합친다`() {
        val menus =
            listOf(
                menu(productName = "산책", serviceType = "DAYCARE", displayOrder = 0),
                menu(productName = "산책", serviceType = "DAYCARE", displayOrder = 1),
            )

        val response = KindergartenPricingResponse.from(sample(menus))

        assertEquals(1, response.productCategories.size)
        assertEquals(
            2,
            response.productCategories
                .single()
                .products.size,
        )
    }
}
