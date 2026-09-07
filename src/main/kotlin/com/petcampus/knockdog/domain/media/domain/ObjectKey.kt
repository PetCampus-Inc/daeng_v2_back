package com.petcampus.knockdog.domain.media.domain

import java.util.UUID

/**
 * S3 object key. presigned URL 문자열과 섞이지 않도록 별도 타입으로 감싸고,
 * 경로 탈출·절대 경로 같은 구조적 위반을 생성 시점에 막는다.
 */
@JvmInline
value class ObjectKey(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "ObjectKey는 비어 있을 수 없습니다." }
        require(!value.startsWith("/")) { "ObjectKey는 절대 경로일 수 없습니다: $value" }
        require(value.split("/").none { it == ".." || it == "." }) { "ObjectKey에 경로 탈출 구획이 있습니다: $value" }
    }

    val filename: String
        get() = value.substringAfterLast('/')

    fun isInTemporaryAreaOf(userCode: String): Boolean = value.startsWith("$TEMPORARY_PREFIX$userCode/")

    companion object {
        private const val TEMPORARY_PREFIX = "tmp/"

        fun temporary(
            userCode: String,
            contentType: MediaContentType,
        ): ObjectKey = ObjectKey("$TEMPORARY_PREFIX$userCode/${UUID.randomUUID()}.${contentType.extension}")
    }
}
