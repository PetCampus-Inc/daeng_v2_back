package com.petcampus.knockdog.domain.media.domain

enum class MediaContentType(
    val mimeType: String,
    val extension: String,
) {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp"),
    ;

    companion object {
        fun forMimeType(mimeType: String): MediaContentType? = entries.find { it.mimeType == mimeType }
    }
}
