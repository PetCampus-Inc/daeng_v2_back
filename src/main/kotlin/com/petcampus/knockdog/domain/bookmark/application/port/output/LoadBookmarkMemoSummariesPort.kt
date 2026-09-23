package com.petcampus.knockdog.domain.bookmark.application.port.output

import java.time.LocalDate

interface LoadBookmarkMemoSummariesPort {
    fun findByUserCode(userCode: String): List<BookmarkMemoSummary>
}

data class BookmarkMemoSummary(
    val kindergartenId: String,
    val memoDate: LocalDate,
)
