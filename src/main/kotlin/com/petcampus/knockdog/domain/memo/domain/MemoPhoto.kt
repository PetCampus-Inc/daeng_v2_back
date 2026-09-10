package com.petcampus.knockdog.domain.memo.domain

class MemoPhoto private constructor(
    val id: Long?,
    val userCode: String,
    val targetId: String,
    val objectKey: String,
    val sortOrder: Int,
) {
    companion object {
        const val MAX_COUNT_PER_TARGET = 5

        fun create(
            userCode: String,
            targetId: String,
            objectKey: String,
            sortOrder: Int,
        ): MemoPhoto = MemoPhoto(null, userCode, targetId, objectKey, sortOrder)

        fun reconstitute(
            id: Long,
            userCode: String,
            targetId: String,
            objectKey: String,
            sortOrder: Int,
        ): MemoPhoto = MemoPhoto(id, userCode, targetId, objectKey, sortOrder)
    }
}
