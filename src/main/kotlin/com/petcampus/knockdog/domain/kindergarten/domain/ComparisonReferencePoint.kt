package com.petcampus.knockdog.domain.kindergarten.domain

data class ComparisonReferencePoint(
    val type: ComparisonReferencePointType,
    val lat: Double,
    val lng: Double,
)

enum class ComparisonReferencePointType {
    HOME,
    OTHER,
}
