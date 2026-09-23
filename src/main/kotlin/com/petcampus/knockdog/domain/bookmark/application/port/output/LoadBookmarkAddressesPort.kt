package com.petcampus.knockdog.domain.bookmark.application.port.output

interface LoadBookmarkAddressesPort {
    fun findByUserCode(userCode: String): List<BookmarkAddress>
}

data class BookmarkAddress(
    val type: String,
    val lat: Double,
    val lng: Double,
)
