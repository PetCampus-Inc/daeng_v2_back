package com.petcampus.knockdog.domain.comparison.application.port.input

interface SaveComparisonHistoryUseCase {
    fun save(
        userCode: String,
        naverPlaceIds: List<String>,
    )
}
