package com.petcampus.knockdog.domain.media.application.port.output

import com.petcampus.knockdog.domain.media.domain.MediaContentType
import com.petcampus.knockdog.domain.media.domain.ObjectKey

interface ObjectStoragePort {
    fun createUploadUrl(
        key: ObjectKey,
        contentType: MediaContentType,
    ): PresignedUrl

    fun createDownloadUrl(key: ObjectKey): PresignedUrl

    fun exists(key: ObjectKey): Boolean

    fun copy(
        source: ObjectKey,
        destination: ObjectKey,
    )

    fun delete(key: ObjectKey)
}

data class PresignedUrl(
    val url: String,
    val expiresIn: Long,
)
