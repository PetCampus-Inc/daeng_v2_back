package com.petcampus.knockdog.domain.comparison.adapter.outbound.persistence

import com.petcampus.knockdog.domain.comparison.application.port.output.LoadComparisonHistoryPort
import com.petcampus.knockdog.domain.comparison.application.port.output.SaveComparisonHistoryPort
import com.petcampus.knockdog.domain.comparison.domain.ComparisonHistory
import com.petcampus.knockdog.domain.comparison.domain.ComparisonHistoryId
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ComparisonHistoryPersistenceAdapter(
    private val comparisonHistoryJpaRepository: ComparisonHistoryJpaRepository,
) : SaveComparisonHistoryPort,
    LoadComparisonHistoryPort {
    @Transactional
    override fun upsert(history: ComparisonHistory) {
        comparisonHistoryJpaRepository.upsert(history.userCode, history.kindergartenIdA, history.kindergartenIdB)
    }

    @Transactional
    override fun softDeleteById(id: Long) {
        comparisonHistoryJpaRepository.softDeleteById(id)
    }

    @Transactional(readOnly = true)
    override fun findRecentByUserCode(
        userCode: String,
        limit: Int,
    ): List<ComparisonHistory> =
        comparisonHistoryJpaRepository
            .findAllByUserCodeAndDeletedAtIsNullOrderByUpdatedAtDescIdDesc(userCode, PageRequest.of(0, limit))
            .map { it.toDomain() }

    @Transactional(readOnly = true)
    override fun findById(id: Long): ComparisonHistory? = comparisonHistoryJpaRepository.findByIdAndDeletedAtIsNull(id)?.toDomain()
}

private fun ComparisonHistoryJpaEntity.toDomain(): ComparisonHistory =
    ComparisonHistory.reconstitute(
        id = ComparisonHistoryId(requireNotNull(id)),
        userCode = userCode,
        kindergartenIdA = kindergartenIdA,
        kindergartenIdB = kindergartenIdB,
        comparedAt = updatedAt,
    )
