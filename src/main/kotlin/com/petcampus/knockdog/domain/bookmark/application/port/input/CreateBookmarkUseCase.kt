package com.petcampus.knockdog.domain.bookmark.application.port.input

interface CreateBookmarkUseCase {
    fun create(
        userCode: String,
        kindergartenId: String,
    )
}
