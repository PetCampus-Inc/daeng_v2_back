package com.petcampus.knockdog.domain.memo.application.port.input

interface GetMemoUseCase {
    fun get(
        userCode: String,
        targetId: String,
    ): MemoView
}

data class MemoView(
    val content: String?,
    val photos: List<PhotoView>,
) {
    data class PhotoView(
        val id: Long,
        val key: String,
        val url: String,
    )
}
