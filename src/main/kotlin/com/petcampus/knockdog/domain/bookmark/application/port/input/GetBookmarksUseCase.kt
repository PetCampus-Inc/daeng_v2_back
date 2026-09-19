package com.petcampus.knockdog.domain.bookmark.application.port.input

import java.time.LocalDate

interface GetBookmarksUseCase {
    fun list(userCode: String): List<BookmarkView>
}

data class BookmarkView(
    val kindergarten: BookmarkKindergartenView,
    val memoDate: LocalDate?,
    val distances: List<BookmarkDistanceView>,
)

data class BookmarkKindergartenView(
    val id: String,
    val name: String,
    val thumbnailS3Key: String?,
    val categories: List<String>,
    val location: String,
    val price: Int,
    val reviewCount: Int,
)

data class BookmarkDistanceView(
    val referencePoint: String,
    val distance: String,
)
