package com.petcampus.knockdog.domain.memo.application.port.output

import com.petcampus.knockdog.domain.memo.domain.Memo
import java.time.LocalDate

interface LoadMemoPort {
    fun findByUserCodeAndTargetId(
        userCode: String,
        targetId: String,
    ): Memo?

    fun findSummariesByUserCode(userCode: String): List<MemoSummary>
}

data class MemoSummary(
    val targetId: String,
    val content: String?,
    val memoDate: LocalDate,
)
