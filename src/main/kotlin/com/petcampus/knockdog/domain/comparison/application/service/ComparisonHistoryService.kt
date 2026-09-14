package com.petcampus.knockdog.domain.comparison.application.service

import com.petcampus.knockdog.domain.comparison.application.ComparisonHistoryErrorCode
import com.petcampus.knockdog.domain.comparison.application.port.input.ComparisonHistoryView
import com.petcampus.knockdog.domain.comparison.application.port.input.DeleteComparisonHistoryUseCase
import com.petcampus.knockdog.domain.comparison.application.port.input.GetComparisonHistoriesUseCase
import com.petcampus.knockdog.domain.comparison.application.port.input.SaveComparisonHistoryUseCase
import com.petcampus.knockdog.domain.comparison.application.port.output.LoadComparisonHistoryPort
import com.petcampus.knockdog.domain.comparison.application.port.output.LoadComparisonKindergartenSummariesPort
import com.petcampus.knockdog.domain.comparison.application.port.output.SaveComparisonHistoryPort
import com.petcampus.knockdog.domain.comparison.domain.ComparisonHistory
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ComparisonHistoryService(
    private val saveComparisonHistoryPort: SaveComparisonHistoryPort,
    private val loadComparisonHistoryPort: LoadComparisonHistoryPort,
    private val loadComparisonKindergartenSummariesPort: LoadComparisonKindergartenSummariesPort,
) : SaveComparisonHistoryUseCase,
    GetComparisonHistoriesUseCase,
    DeleteComparisonHistoryUseCase {
    @Transactional
    override fun save(
        userCode: String,
        naverPlaceIds: List<String>,
    ) {
        require(naverPlaceIds.size == TARGET_COUNT) { "비교 히스토리는 유치원 2곳으로만 저장한다." }
        saveComparisonHistoryPort.upsert(
            ComparisonHistory.create(userCode, naverPlaceIds[0], naverPlaceIds[1]),
        )
    }

    @Transactional(readOnly = true)
    override fun list(
        userCode: String,
        limit: Int,
    ): List<ComparisonHistoryView> {
        val histories = loadComparisonHistoryPort.findRecentByUserCode(userCode, clamp(limit))
        val summariesById =
            loadComparisonKindergartenSummariesPort
                .findByNaverPlaceIds(histories.flatMap { it.naverPlaceIds }.distinct())
                .associateBy { it.id }

        return histories.map { history ->
            ComparisonHistoryView(
                id = requireNotNull(history.id).value,
                kindergartens = history.naverPlaceIds.mapNotNull { summariesById[it] },
                comparedAt = requireNotNull(history.comparedAt),
            )
        }
    }

    @Transactional
    override fun delete(
        userCode: String,
        historyId: Long,
    ) {
        val history =
            loadComparisonHistoryPort.findById(historyId)
                ?: throw BusinessException(ComparisonHistoryErrorCode.NOT_FOUND)
        if (!history.isOwnedBy(userCode)) {
            throw BusinessException(ComparisonHistoryErrorCode.NOT_OWNER)
        }
        saveComparisonHistoryPort.softDeleteById(historyId)
    }

    private fun clamp(limit: Int): Int = if (limit <= 0) DEFAULT_LIMIT else minOf(limit, MAX_LIMIT)

    companion object {
        private const val TARGET_COUNT = 2
        private const val DEFAULT_LIMIT = 10
        private const val MAX_LIMIT = 50
    }
}
