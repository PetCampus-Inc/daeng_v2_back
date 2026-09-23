package com.petcampus.knockdog.domain.bookmark.adapter.outbound.kindergarten

import com.petcampus.knockdog.domain.bookmark.application.port.output.BookmarkKindergarten
import com.petcampus.knockdog.domain.bookmark.application.port.output.LoadBookmarkKindergartensPort
import com.petcampus.knockdog.domain.kindergarten.application.port.output.KindergartenCardSummary
import com.petcampus.knockdog.domain.kindergarten.application.port.output.LoadKindergartenPort
import org.springframework.stereotype.Component

@Component
class BookmarkKindergartenAdapter(
    private val loadKindergartenPort: LoadKindergartenPort,
) : LoadBookmarkKindergartensPort {
    override fun findById(kindergartenId: String): BookmarkKindergarten? =
        loadKindergartenPort.findCardSummariesByNaverPlaceIds(listOf(kindergartenId)).singleOrNull()?.toBookmarkKindergarten()

    override fun findByIds(kindergartenIds: List<String>): List<BookmarkKindergarten> {
        if (kindergartenIds.isEmpty()) return emptyList()
        return loadKindergartenPort.findCardSummariesByNaverPlaceIds(kindergartenIds).map { it.toBookmarkKindergarten() }
    }
}

private fun KindergartenCardSummary.toBookmarkKindergarten(): BookmarkKindergarten =
    BookmarkKindergarten(
        id = id,
        name = name,
        thumbnailS3Key = thumbnailS3Key,
        categories = categories,
        location = address,
        price = lowestPrice,
        reviewCount = blogReviewCount,
        lat = lat,
        lng = lng,
        closed = closed,
    )
