package com.petcampus.knockdog.domain.bookmark.application.port.output

import com.petcampus.knockdog.domain.bookmark.domain.Bookmark

interface LoadBookmarkPort {
    fun findAllByUserCode(userCode: String): List<Bookmark>
}
