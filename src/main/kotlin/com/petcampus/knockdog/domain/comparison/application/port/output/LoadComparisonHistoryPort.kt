package com.petcampus.knockdog.domain.comparison.application.port.output

import com.petcampus.knockdog.domain.comparison.domain.ComparisonHistory

interface LoadComparisonHistoryPort {
    fun findRecentByUserCode(
        userCode: String,
        limit: Int,
    ): List<ComparisonHistory>

    fun findById(id: Long): ComparisonHistory?
}
