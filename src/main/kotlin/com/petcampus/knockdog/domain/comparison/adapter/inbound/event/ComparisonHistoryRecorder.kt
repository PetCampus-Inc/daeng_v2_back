package com.petcampus.knockdog.domain.comparison.adapter.inbound.event

import com.petcampus.knockdog.domain.comparison.application.port.input.SaveComparisonHistoryUseCase
import com.petcampus.knockdog.domain.kindergarten.application.event.KindergartensComparedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ComparisonHistoryRecorder(
    private val saveComparisonHistoryUseCase: SaveComparisonHistoryUseCase,
) {
    @EventListener
    fun on(event: KindergartensComparedEvent) {
        saveComparisonHistoryUseCase.save(event.userCode, event.naverPlaceIds)
    }
}
