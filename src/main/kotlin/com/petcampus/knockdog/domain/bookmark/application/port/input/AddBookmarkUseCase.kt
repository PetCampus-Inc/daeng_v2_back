package com.petcampus.knockdog.domain.bookmark.application.port.input

import com.petcampus.knockdog.domain.bookmark.domain.BookmarkJpaEntity

interface AddBookmarkUseCase {
    fun add(command: AddBookmarkCommand): BookmarkJpaEntity
}

data class AddBookmarkCommand(
    val ownerId: String,
    val kindergartenId: String,
)
