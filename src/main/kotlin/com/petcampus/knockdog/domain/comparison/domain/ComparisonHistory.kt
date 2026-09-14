package com.petcampus.knockdog.domain.comparison.domain

import java.time.LocalDateTime

class ComparisonHistory private constructor(
    val id: ComparisonHistoryId?,
    val userCode: String,
    val kindergartenIdA: String,
    val kindergartenIdB: String,
    val comparedAt: LocalDateTime?,
) {
    val naverPlaceIds: List<String>
        get() = listOf(kindergartenIdA, kindergartenIdB)

    fun isOwnedBy(userCode: String): Boolean = this.userCode == userCode

    companion object {
        fun create(
            userCode: String,
            firstNaverPlaceId: String,
            secondNaverPlaceId: String,
        ): ComparisonHistory {
            require(firstNaverPlaceId != secondNaverPlaceId) { "비교 대상 유치원이 같을 수 없습니다." }
            val (a, b) = listOf(firstNaverPlaceId, secondNaverPlaceId).sorted()
            return ComparisonHistory(null, userCode, a, b, null)
        }

        fun reconstitute(
            id: ComparisonHistoryId,
            userCode: String,
            kindergartenIdA: String,
            kindergartenIdB: String,
            comparedAt: LocalDateTime,
        ): ComparisonHistory = ComparisonHistory(id, userCode, kindergartenIdA, kindergartenIdB, comparedAt)
    }
}
