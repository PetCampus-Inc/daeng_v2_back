package com.petcampus.knockdog.domain.bookmark.application.port.output

interface LoadBookmarkKindergartensPort {
    fun findById(kindergartenId: String): BookmarkKindergarten?

    fun findByIds(kindergartenIds: List<String>): List<BookmarkKindergarten>
}

data class BookmarkKindergarten(
    val id: String,
    val name: String,
    val thumbnailS3Key: String?,
    val categories: List<String>,
    val location: String,
    val price: Int,
    val reviewCount: Int,
    val lat: Double?,
    val lng: Double?,
    val closed: Boolean,
)
