package com.petcampus.knockdog.domain.media.domain

import java.util.UUID

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
