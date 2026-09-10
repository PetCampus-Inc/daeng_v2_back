package com.petcampus.knockdog.domain.kindergarten.domain

import kotlin.math.roundToInt

object KindergartenPricingComparisonCalculator {
    private const val COUNT_TICKET = "COUNT_TICKET"
    private const val MONTHLY_TICKET = "MONTHLY_TICKET"

    fun calculate(menus: List<KindergartenMenu>): KindergartenPricingComparison? {
        if (menus.isEmpty()) return null

        return KindergartenPricingComparison(
            countHourlyAvg = hourlyAverage(menus, COUNT_TICKET),
            monthlyHourlyAvg = hourlyAverage(menus, MONTHLY_TICKET),
            products =
                menus
                    .groupBy { it.serviceType }
                    .mapNotNull { (serviceType, group) -> productOf(serviceType, group) },
        )
    }

    private fun productOf(
        serviceType: String,
        menus: List<KindergartenMenu>,
    ): KindergartenPricingComparison.Product? {
        val priced = menus.mapNotNull { menu -> menu.price?.let { menu to it } }
        if (priced.isEmpty()) return null

        val cheapest = priced.minBy { it.second }.first
        val priciest = priced.maxBy { it.second }.first

        return KindergartenPricingComparison.Product(
            serviceType = serviceType,
            min = KindergartenPricingComparison.PriceItem(cheapest.productName, cheapest.price ?: 0),
            max = KindergartenPricingComparison.PriceItem(priciest.productName, priciest.price ?: 0),
            countTicketAvg = hourlyAverage(menus, COUNT_TICKET),
            monthlyHourlyAvg = hourlyAverage(menus, MONTHLY_TICKET),
        )
    }

    private fun hourlyAverage(
        menus: List<KindergartenMenu>,
        productType: String,
    ): Int {
        val hourlyPrices = menus.filter { it.productType == productType }.mapNotNull { it.hourlyPrice }
        if (hourlyPrices.isEmpty()) return 0
        return hourlyPrices.average().roundToInt()
    }
}
