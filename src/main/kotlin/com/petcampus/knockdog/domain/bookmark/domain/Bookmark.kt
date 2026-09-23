package com.petcampus.knockdog.domain.bookmark.domain

class Bookmark private constructor(
    val id: BookmarkId?,
    val userCode: String,
    val kindergartenId: String,
) {
    companion object {
        fun create(
            userCode: String,
            kindergartenId: String,
        ): Bookmark {
            require(userCode.isNotBlank()) { "회원 식별자는 비어 있을 수 없습니다." }
            require(kindergartenId.isNotBlank()) { "유치원 식별자는 비어 있을 수 없습니다." }
            return Bookmark(null, userCode, kindergartenId)
        }

        fun reconstitute(
            id: BookmarkId,
            userCode: String,
            kindergartenId: String,
        ): Bookmark = Bookmark(id, userCode, kindergartenId)
    }
}
