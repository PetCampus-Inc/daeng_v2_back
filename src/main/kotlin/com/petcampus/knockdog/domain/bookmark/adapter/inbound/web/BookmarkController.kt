package com.petcampus.knockdog.domain.bookmark.adapter.inbound.web

import com.petcampus.knockdog.domain.bookmark.application.port.input.AddBookmarkCommand
import com.petcampus.knockdog.domain.bookmark.application.port.input.AddBookmarkUseCase
import com.petcampus.knockdog.domain.bookmark.application.port.input.GetBookmarksUseCase
import com.petcampus.knockdog.domain.bookmark.domain.BookmarkJpaEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/bookmarks")
class BookmarkController(
    private val addBookmarkUseCase: AddBookmarkUseCase,
    private val getBookmarksUseCase: GetBookmarksUseCase,
) {
    @PostMapping
    fun add(
        @RequestBody request: AddBookmarkRequest,
    ): ResponseEntity<BookmarkResponse> {
        val bookmark = addBookmarkUseCase.add(AddBookmarkCommand(request.ownerId, request.kindergartenId))
        return ResponseEntity.status(HttpStatus.CREATED).body(BookmarkResponse.from(bookmark))
    }

    @GetMapping
    fun getByOwner(
        @RequestParam ownerId: String,
    ): List<BookmarkResponse> = getBookmarksUseCase.getByOwnerId(ownerId).map(BookmarkResponse::from)
}

data class AddBookmarkRequest(
    val ownerId: String,
    val kindergartenId: String,
)

data class BookmarkResponse(
    val id: String,
    val ownerId: String,
    val kindergartenId: String,
) {
    companion object {
        fun from(entity: BookmarkJpaEntity): BookmarkResponse = BookmarkResponse(entity.id, entity.ownerId, entity.kindergartenId)
    }
}
