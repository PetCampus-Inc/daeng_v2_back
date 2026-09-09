package com.petcampus.knockdog.domain.media.domain

enum class MediaPurpose {
    PROFILE_IMAGE,
    ;

    fun permanentKey(
        userCode: String,
        filename: String,
    ): ObjectKey =
        when (this) {
            PROFILE_IMAGE -> ObjectKey("user/$userCode/$filename")
        }

    companion object {
        fun from(value: String): MediaPurpose? = entries.find { it.name == value }
    }
}
