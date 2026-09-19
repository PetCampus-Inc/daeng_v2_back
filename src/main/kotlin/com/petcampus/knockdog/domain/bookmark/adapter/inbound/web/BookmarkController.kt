package com.petcampus.knockdog.domain.bookmark.adapter.inbound.web

import com.petcampus.knockdog.domain.bookmark.application.port.input.BookmarkView
import com.petcampus.knockdog.domain.bookmark.application.port.input.CreateBookmarkUseCase
import com.petcampus.knockdog.domain.bookmark.application.port.input.DeleteBookmarkUseCase
import com.petcampus.knockdog.domain.bookmark.application.port.input.GetBookmarksUseCase
import com.petcampus.knockdog.global.response.Response
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class BookmarkController(
    private val createBookmarkUseCase: CreateBookmarkUseCase,
    private val deleteBookmarkUseCase: DeleteBookmarkUseCase,
    private val getBookmarksUseCase: GetBookmarksUseCase,
) {
    @GetMapping("/api/v1/users/me/bookmarks")
    fun list(
        @AuthenticationPrincipal userCode: String,
    ): Response<List<BookmarkResponse>> = Response.success(getBookmarksUseCase.list(userCode).map { BookmarkResponse.from(it) })

    @PutMapping("/api/v1/kindergartens/{kindergartenId}/bookmark")
    fun create(
        @AuthenticationPrincipal userCode: String,
        @PathVariable kindergartenId: String,
    ): Response<Unit> {
        createBookmarkUseCase.create(userCode, kindergartenId)
        return Response.success()
    }

    @DeleteMapping("/api/v1/kindergartens/{kindergartenId}/bookmark")
    fun delete(
        @AuthenticationPrincipal userCode: String,
        @PathVariable kindergartenId: String,
    ): Response<Unit> {
        deleteBookmarkUseCase.delete(userCode, kindergartenId)
        return Response.success()
    }
}

data class BookmarkResponse(
    val kindergarten: Kindergarten,
    val memoDate: LocalDate?,
    val distances: List<Distance>,
) {
    data class Kindergarten(
        val id: String,
        val name: String,
        val thumbnailS3Key: String?,
        val categories: List<String>,
        val location: String,
        val price: Int,
        val reviewCount: Int,
    )

    data class Distance(
        val referencePoint: String,
        val distance: String,
    )

    companion object {
        fun from(view: BookmarkView): BookmarkResponse =
            BookmarkResponse(
                kindergarten =
                    Kindergarten(
                        id = view.kindergarten.id,
                        name = view.kindergarten.name,
                        thumbnailS3Key = view.kindergarten.thumbnailS3Key,
                        categories = view.kindergarten.categories,
                        location = view.kindergarten.location,
                        price = view.kindergarten.price,
                        reviewCount = view.kindergarten.reviewCount,
                    ),
                memoDate = view.memoDate,
                distances = view.distances.map { Distance(it.referencePoint, it.distance) },
            )
    }
}
