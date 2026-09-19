package com.petcampus.knockdog.domain.bookmark.application

import com.petcampus.knockdog.global.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class BookmarkErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    CLOSED_SCHOOL(HttpStatus.BAD_REQUEST, "BOOKMARK_CLOSED_SCHOOL", "폐업한 유치원은 보관할 수 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "BOOKMARK_NOT_FOUND", "해당 북마크가 없습니다."),
}
