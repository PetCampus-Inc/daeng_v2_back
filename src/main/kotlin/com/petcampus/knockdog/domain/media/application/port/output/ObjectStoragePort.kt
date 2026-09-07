package com.petcampus.knockdog.domain.media.application.port.output

import com.petcampus.knockdog.domain.media.domain.MediaContentType
import com.petcampus.knockdog.domain.media.domain.ObjectKey

/**
 * object storage 아웃바운드 포트. S3에 의존하지 않는 계약이며,
 * presigned URL 만료 시간 등 "어떻게"는 어댑터가 설정으로 결정한다.
 */
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

/** presigned URL과 남은 유효 시간(초). */
data class PresignedUrl(
    val url: String,
    val expiresIn: Long,
)
