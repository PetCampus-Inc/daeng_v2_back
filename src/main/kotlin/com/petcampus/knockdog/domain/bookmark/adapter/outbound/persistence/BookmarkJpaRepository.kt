package com.petcampus.knockdog.domain.bookmark.adapter.outbound.persistence

import com.petcampus.knockdog.domain.bookmark.domain.BookmarkJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BookmarkJpaRepository : JpaRepository<BookmarkJpaEntity, String> {
    fun findByOwnerId(ownerId: String): List<BookmarkJpaEntity>
}
