package com.petcampus.knockdog.domain.bookmark.adapter.outbound.persistence

import com.petcampus.knockdog.domain.bookmark.application.port.output.LoadBookmarkPort
import com.petcampus.knockdog.domain.bookmark.application.port.output.SaveBookmarkPort
import com.petcampus.knockdog.domain.bookmark.domain.Bookmark
import com.petcampus.knockdog.domain.bookmark.domain.BookmarkId
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BookmarkPersistenceAdapter(
    private val bookmarkJpaRepository: BookmarkJpaRepository,
) : LoadBookmarkPort,
    SaveBookmarkPort {
    @Transactional(readOnly = true)
    override fun findAllByUserCode(userCode: String): List<Bookmark> =
        bookmarkJpaRepository.findAllByUserCodeOrderByCreatedAtDescIdDesc(userCode).map { it.toDomain() }

    @Transactional
    override fun createIfAbsent(bookmark: Bookmark) {
        bookmarkJpaRepository.createIfAbsent(bookmark.userCode, bookmark.kindergartenId)
    }

    @Transactional
    override fun delete(
        userCode: String,
        kindergartenId: String,
    ): Boolean = bookmarkJpaRepository.deleteByUserCodeAndKindergartenId(userCode, kindergartenId) > 0
}

private fun BookmarkJpaEntity.toDomain(): Bookmark =
    Bookmark.reconstitute(
        id = BookmarkId(requireNotNull(id)),
        userCode = userCode,
        kindergartenId = kindergartenId,
    )
