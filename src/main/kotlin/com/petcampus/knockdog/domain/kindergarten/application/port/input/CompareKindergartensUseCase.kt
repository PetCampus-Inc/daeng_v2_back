package com.petcampus.knockdog.domain.kindergarten.application.port.input

import com.petcampus.knockdog.domain.kindergarten.domain.ComparisonReferencePoint
import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten

interface CompareKindergartensUseCase {
    fun compare(command: CompareKindergartensCommand): CompareKindergartensResult
}

data class CompareKindergartensCommand(
    val naverPlaceIds: List<String>,
    val userCode: String?,
    val lat: Double?,
    val lng: Double?,
)

data class CompareKindergartensResult(
    val kindergartens: List<Kindergarten>,
    val referencePoints: List<ComparisonReferencePoint>,
)
