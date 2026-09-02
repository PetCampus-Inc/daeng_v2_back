package com.petcampus.knockdog.domain.kindergarten.domain

/** 요금 상품(메뉴) 하나. `price_and_product.json`의 행 하나에 대응한다. */
data class KindergartenMenu(
    val productType: String,
    val serviceType: String,
    val productName: String,
    val unit: Double?,
    val unitStr: String?,
    val unitType: String?,
    val weightRange: String?,
    val price: Int?,
    val hourlyPrice: Int?,
    val isMinPrice: Boolean,
    val isMaxPrice: Boolean,
    val totalDurationStr: String?,
    val totalDurationMinutes: Int?,
    val displayOrder: Int,
)
