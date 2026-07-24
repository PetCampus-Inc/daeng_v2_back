package com.petcampus.knockdog.domain.bookmark.application.service

import com.petcampus.knockdog.domain.bookmark.application.port.input.AddBookmarkCommand
import com.petcampus.knockdog.domain.bookmark.application.port.input.AddBookmarkUseCase
import com.petcampus.knockdog.domain.bookmark.application.port.input.GetBookmarksUseCase
import com.petcampus.knockdog.domain.bookmark.application.port.output.BookmarkPort
import com.petcampus.knockdog.domain.bookmark.domain.BookmarkJpaEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BookmarkService(
    private val bookmarkPort: BookmarkPort,
) : AddBookmarkUseCase,
    GetBookmarksUseCase {
    @Transactional
    override fun add(command: AddBookmarkCommand): BookmarkJpaEntity {
        val bookmark =
            BookmarkJpaEntity(
                id = UUID.randomUUID().toString(),
                ownerId = command.ownerId,
                kindergartenId = command.kindergartenId,
            )
        return bookmarkPort.save(bookmark)
    }

    @Transactional(readOnly = true)
    override fun getByOwnerId(ownerId: String): List<BookmarkJpaEntity> = bookmarkPort.findByOwnerId(ownerId)
}
