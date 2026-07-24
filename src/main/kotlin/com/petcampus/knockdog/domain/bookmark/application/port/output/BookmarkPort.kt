package com.petcampus.knockdog.domain.bookmark.application.port.output

import com.petcampus.knockdog.domain.bookmark.domain.BookmarkJpaEntity

interface BookmarkPort {
    fun save(bookmark: BookmarkJpaEntity): BookmarkJpaEntity

    fun findByOwnerId(ownerId: String): List<BookmarkJpaEntity>
}
