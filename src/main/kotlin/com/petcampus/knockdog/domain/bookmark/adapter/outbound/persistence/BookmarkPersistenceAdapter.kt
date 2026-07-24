package com.petcampus.knockdog.domain.bookmark.adapter.outbound.persistence

import com.petcampus.knockdog.domain.bookmark.application.port.output.BookmarkPort
import com.petcampus.knockdog.domain.bookmark.domain.BookmarkJpaEntity
import org.springframework.stereotype.Component

/** 실용형: 매퍼 없이 엔티티를 그대로 저장/조회한다. */
@Component
class BookmarkPersistenceAdapter(
    private val bookmarkJpaRepository: BookmarkJpaRepository,
) : BookmarkPort {
    override fun save(bookmark: BookmarkJpaEntity): BookmarkJpaEntity = bookmarkJpaRepository.save(bookmark)

    override fun findByOwnerId(ownerId: String): List<BookmarkJpaEntity> = bookmarkJpaRepository.findByOwnerId(ownerId)
}
