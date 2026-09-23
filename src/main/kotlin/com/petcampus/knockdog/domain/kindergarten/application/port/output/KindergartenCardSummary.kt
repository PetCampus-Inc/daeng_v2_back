package com.petcampus.knockdog.domain.kindergarten.application.port.output

data class KindergartenCardSummary(
    val id: String,
    val name: String,
    val thumbnailS3Key: String?,
    val categories: List<String>,
    val address: String,
    val lowestPrice: Int,
    val blogReviewCount: Int,
    val lat: Double?,
    val lng: Double?,
    val closed: Boolean,
)
