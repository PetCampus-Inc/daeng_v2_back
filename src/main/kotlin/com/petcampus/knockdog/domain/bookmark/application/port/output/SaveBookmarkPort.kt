package com.petcampus.knockdog.domain.bookmark.application.port.output

import com.petcampus.knockdog.domain.bookmark.domain.Bookmark

interface SaveBookmarkPort {
    fun createIfAbsent(bookmark: Bookmark)

    fun delete(
        userCode: String,
        kindergartenId: String,
    ): Boolean
}
