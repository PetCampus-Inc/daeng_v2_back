package com.petcampus.knockdog.domain.memo.application.port.output

import com.petcampus.knockdog.domain.memo.domain.FreeMemo
import java.time.LocalDate

interface LoadFreeMemoPort {
    fun findByUserCodeAndTargetId(
        userCode: String,
        targetId: String,
    ): FreeMemo?

    fun findSummariesByUserCode(userCode: String): List<MemoSummary>
}

data class MemoSummary(
    val targetId: String,
    val content: String?,
    val memoDate: LocalDate,
)
