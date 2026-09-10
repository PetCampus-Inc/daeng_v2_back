package com.petcampus.knockdog.domain.kindergarten.domain

data class KindergartenPricingComparison(
    val countHourlyAvg: Int,
    val monthlyHourlyAvg: Int,
    val products: List<Product>,
) {
    data class Product(
        val serviceType: String,
        val min: PriceItem,
        val max: PriceItem,
        val countTicketAvg: Int,
        val monthlyHourlyAvg: Int,
    )

    data class PriceItem(
        val name: String,
        val price: Int,
    )
}
