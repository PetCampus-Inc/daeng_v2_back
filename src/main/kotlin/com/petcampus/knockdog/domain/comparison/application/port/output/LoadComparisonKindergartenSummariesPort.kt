package com.petcampus.knockdog.domain.comparison.application.port.output

interface LoadComparisonKindergartenSummariesPort {
    fun findByNaverPlaceIds(naverPlaceIds: List<String>): List<ComparisonKindergartenSummary>
}

data class ComparisonKindergartenSummary(
    val id: String,
    val name: String,
    val thumbnailS3Key: String?,
    val categories: List<String>,
)
