package com.petcampus.knockdog.domain.comparison.application.port.input

import com.petcampus.knockdog.domain.comparison.application.port.output.ComparisonKindergartenSummary
import java.time.LocalDateTime

interface GetComparisonHistoriesUseCase {
    fun list(
        userCode: String,
        limit: Int,
    ): List<ComparisonHistoryView>
}

data class ComparisonHistoryView(
    val id: Long,
    val kindergartens: List<ComparisonKindergartenSummary>,
    val comparedAt: LocalDateTime,
)
