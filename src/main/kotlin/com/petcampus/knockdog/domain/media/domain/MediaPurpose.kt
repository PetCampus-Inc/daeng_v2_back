package com.petcampus.knockdog.domain.media.domain

enum class MediaPurpose {
    PROFILE_IMAGE,
    MEMO_ATTACHMENT,
    ;

    fun permanentKey(
        userCode: String,
        filename: String,
    ): ObjectKey =
        when (this) {
            PROFILE_IMAGE -> ObjectKey("user/$userCode/$filename")
            MEMO_ATTACHMENT -> ObjectKey("memo/$userCode/$filename")
        }

    companion object {
        fun from(value: String): MediaPurpose? = entries.find { it.name == value }
    }
}
