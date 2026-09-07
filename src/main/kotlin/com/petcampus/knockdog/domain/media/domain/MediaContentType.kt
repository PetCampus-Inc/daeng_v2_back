package com.petcampus.knockdog.domain.media.domain

/**
 * 업로드를 허용하는 이미지 MIME 타입과, object key에 붙일 확장자의 대응.
 * 레거시는 content-type 검증이 없었다 — 신규 서버는 허용 목록으로 좁힌다.
 */
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
