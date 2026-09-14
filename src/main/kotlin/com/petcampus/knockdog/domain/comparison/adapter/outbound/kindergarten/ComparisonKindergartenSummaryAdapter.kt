package com.petcampus.knockdog.domain.comparison.adapter.outbound.kindergarten

import com.petcampus.knockdog.domain.comparison.application.port.output.ComparisonKindergartenSummary
import com.petcampus.knockdog.domain.comparison.application.port.output.LoadComparisonKindergartenSummariesPort
import com.petcampus.knockdog.domain.kindergarten.application.port.output.LoadKindergartenPort
import org.springframework.stereotype.Component

@Component
class ComparisonKindergartenSummaryAdapter(
    private val loadKindergartenPort: LoadKindergartenPort,
) : LoadComparisonKindergartenSummariesPort {
    override fun findByNaverPlaceIds(naverPlaceIds: List<String>): List<ComparisonKindergartenSummary> {
        if (naverPlaceIds.isEmpty()) return emptyList()
        return loadKindergartenPort.findByNaverPlaceIds(naverPlaceIds).map { kindergarten ->
            ComparisonKindergartenSummary(
                id = kindergarten.naverPlaceId,
                name = kindergarten.name,
                thumbnailS3Key = kindergarten.thumbnailS3Key,
                categories = kindergarten.categories.map { it.value },
            )
        }
    }
}
