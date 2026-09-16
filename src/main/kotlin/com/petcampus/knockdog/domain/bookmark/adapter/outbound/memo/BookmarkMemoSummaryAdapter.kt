package com.petcampus.knockdog.domain.bookmark.adapter.outbound.memo

import com.petcampus.knockdog.domain.bookmark.application.port.output.BookmarkMemoSummary
import com.petcampus.knockdog.domain.bookmark.application.port.output.LoadBookmarkMemoSummariesPort
import com.petcampus.knockdog.domain.memo.application.port.output.LoadMemoPort
import org.springframework.stereotype.Component

@Component
class BookmarkMemoSummaryAdapter(
    private val loadMemoPort: LoadMemoPort,
) : LoadBookmarkMemoSummariesPort {
    override fun findByUserCode(userCode: String): List<BookmarkMemoSummary> =
        loadMemoPort
            .findSummariesByUserCode(userCode)
            .map { BookmarkMemoSummary(kindergartenId = it.targetId, memoDate = it.memoDate) }
}
