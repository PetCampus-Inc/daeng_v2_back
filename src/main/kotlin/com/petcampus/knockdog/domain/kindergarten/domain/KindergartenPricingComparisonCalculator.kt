package com.petcampus.knockdog.domain.kindergarten.domain

import kotlin.math.roundToInt

object KindergartenPricingComparisonCalculator {
    private const val COUNT_TICKET = "COUNT_TICKET"
    private const val MONTHLY_TICKET = "MONTHLY_TICKET"

    fun calculate(menus: List<KindergartenMenu>): KindergartenPricingComparison? {
        if (menus.isEmpty()) return null

        val countHourlyAvg = hourlyAverage(menus, COUNT_TICKET)
        val monthlyHourlyAvg = hourlyAverage(menus, MONTHLY_TICKET)
        val products =
            menus
                .groupBy { it.serviceType }
                .mapNotNull { (serviceType, group) -> productOf(serviceType, group) }

        if (products.isEmpty() && countHourlyAvg == 0 && monthlyHourlyAvg == 0) return null

        return KindergartenPricingComparison(countHourlyAvg, monthlyHourlyAvg, products)
    }

    private fun productOf(
        serviceType: String,
        menus: List<KindergartenMenu>,
    ): KindergartenPricingComparison.Product? {
        val pricedMenus = menus.mapNotNull { menu -> menu.price?.let { price -> menu.productName to price } }
        if (pricedMenus.isEmpty()) return null

        val cheapest = pricedMenus.minBy { it.second }
        val priciest = pricedMenus.maxBy { it.second }

        return KindergartenPricingComparison.Product(
            serviceType = serviceType,
            min = KindergartenPricingComparison.PriceItem(cheapest.first, cheapest.second),
            max = KindergartenPricingComparison.PriceItem(priciest.first, priciest.second),
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
