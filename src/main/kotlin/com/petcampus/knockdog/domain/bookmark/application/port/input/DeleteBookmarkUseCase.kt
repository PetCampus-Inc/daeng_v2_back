package com.petcampus.knockdog.domain.bookmark.application.port.input

interface DeleteBookmarkUseCase {
    fun delete(
        userCode: String,
        kindergartenId: String,
    )
}
