package com.petcampus.knockdog.domain.comparison.adapter.inbound.web

import com.petcampus.knockdog.domain.comparison.application.port.input.ComparisonHistoryView
import java.time.LocalDateTime

data class ComparisonHistoryResponse(
    val id: Long,
    val kindergartens: List<KindergartenSummary>,
    val comparedAt: LocalDateTime,
) {
    data class KindergartenSummary(
        val id: String,
        val name: String,
        val thumbnailS3Key: String?,
        val categories: List<String>,
    )

    companion object {
        fun from(view: ComparisonHistoryView): ComparisonHistoryResponse =
            ComparisonHistoryResponse(
                id = view.id,
                kindergartens =
                    view.kindergartens.map {
                        KindergartenSummary(it.id, it.name, it.thumbnailS3Key, it.categories)
                    },
                comparedAt = view.comparedAt,
            )
    }
}
