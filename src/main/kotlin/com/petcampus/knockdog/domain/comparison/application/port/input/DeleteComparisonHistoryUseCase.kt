package com.petcampus.knockdog.domain.comparison.application.port.input

interface DeleteComparisonHistoryUseCase {
    fun delete(
        userCode: String,
        historyId: Long,
    )
}
