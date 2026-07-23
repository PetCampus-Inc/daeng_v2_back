package com.petcampus.knockdog.domain.bookmark.application.port.input

import com.petcampus.knockdog.domain.bookmark.domain.BookmarkJpaEntity

interface GetBookmarksUseCase {
    fun getByOwnerId(ownerId: String): List<BookmarkJpaEntity>
}
