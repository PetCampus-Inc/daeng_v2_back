package com.petcampus.knockdog.domain.comparison.application.port.output

import com.petcampus.knockdog.domain.comparison.domain.ComparisonHistory

interface SaveComparisonHistoryPort {
    fun upsert(history: ComparisonHistory)

    fun softDeleteById(id: Long)
}
