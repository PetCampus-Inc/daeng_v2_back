package com.petcampus.knockdog.domain.kindergarten.domain

data class KindergartenMenu(
    val productType: String,
    val serviceType: String,
    val productName: String,
    val unit: Double?,
    val unitLabel: String?,
    val unitType: String?,
    val weightRange: String?,
    val price: Int?,
    val hourlyPrice: Int?,
    val isMinPrice: Boolean,
    val isMaxPrice: Boolean,
    val totalDurationLabel: String?,
    val totalDurationMinutes: Int?,
    val displayOrder: Int,
)
