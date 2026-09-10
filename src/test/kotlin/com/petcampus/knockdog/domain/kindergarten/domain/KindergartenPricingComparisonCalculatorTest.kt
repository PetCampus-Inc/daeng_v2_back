package com.petcampus.knockdog.domain.kindergarten.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KindergartenPricingComparisonCalculatorTest {
    private fun menu(
        productType: String,
        serviceType: String,
        productName: String = "상품",
        price: Int? = null,
        hourlyPrice: Int? = null,
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

    @Test
    fun `메뉴가 없으면 null을 반환한다`() {
        assertNull(KindergartenPricingComparisonCalculator.calculate(emptyList()))
    }

    @Test
    fun `서비스종류별로 price 최저-최고 메뉴를 뽑는다`() {
        val menus =
            listOf(
                menu("COUNT_TICKET", "DAYCARE", "산책", price = 10000),
                menu("COUNT_TICKET", "DAYCARE", "종일반", price = 60000),
                menu("COUNT_TICKET", "DAYCARE", "반일반", price = 30000),
            )

        val result = KindergartenPricingComparisonCalculator.calculate(menus)!!

        val daycare = result.products.single { it.serviceType == "DAYCARE" }
        assertEquals(KindergartenPricingComparison.PriceItem("산책", 10000), daycare.min)
        assertEquals(KindergartenPricingComparison.PriceItem("종일반", 60000), daycare.max)
    }

    @Test
    fun `정책별 시간당 평균가는 hourlyPrice의 반올림 평균이다`() {
        val menus =
            listOf(
                menu("COUNT_TICKET", "DAYCARE", price = 1, hourlyPrice = 6000),
                menu("COUNT_TICKET", "DAYCARE", price = 1, hourlyPrice = 8000),
                menu("COUNT_TICKET", "DAYCARE", price = 1, hourlyPrice = 3847),
                menu("MONTHLY_TICKET", "DAYCARE", price = 1, hourlyPrice = 5000),
            )

        val result = KindergartenPricingComparisonCalculator.calculate(menus)!!

        assertEquals(5949, result.countHourlyAvg)
        assertEquals(5000, result.monthlyHourlyAvg)

        val daycare = result.products.single { it.serviceType == "DAYCARE" }
        assertEquals(5949, daycare.countTicketAvg)
        assertEquals(5000, daycare.monthlyHourlyAvg)
    }

    @Test
    fun `hourlyPrice가 없는 행은 평균에서 제외한다`() {
        val menus =
            listOf(
                menu("COUNT_TICKET", "DAYCARE", price = 1, hourlyPrice = 4000),
                menu("COUNT_TICKET", "DAYCARE", price = 1, hourlyPrice = null),
                menu("COUNT_TICKET", "DAYCARE", price = 1, hourlyPrice = 6000),
            )

        val result = KindergartenPricingComparisonCalculator.calculate(menus)!!

        assertEquals(5000, result.countHourlyAvg)
    }

    @Test
    fun `해당 정책의 행이 없으면 평균은 0이다`() {
        val menus = listOf(menu("COUNT_TICKET", "TRAINING", price = 50000, hourlyPrice = 10000))

        val result = KindergartenPricingComparisonCalculator.calculate(menus)!!

        assertEquals(0, result.monthlyHourlyAvg)
        assertEquals(0, result.products.single().monthlyHourlyAvg)
    }

    @Test
    fun `price가 전부 없는 서비스종류는 products에서 빠진다`() {
        val menus =
            listOf(
                menu("COUNT_TICKET", "DAYCARE", price = 30000, hourlyPrice = 5000),
                menu("MEMBERSHIP", "MEMBERSHIP", price = null, hourlyPrice = null),
            )

        val result = KindergartenPricingComparisonCalculator.calculate(menus)!!

        assertEquals(listOf("DAYCARE"), result.products.map { it.serviceType })
    }
}
