package com.petcampus.knockdog.domain.memo.application.port.input

interface GetFreeMemoUseCase {
    fun get(
        userCode: String,
        targetId: String,
    ): FreeMemoView
}

data class FreeMemoView(
    val content: String?,
    val photos: List<PhotoView>,
) {
    data class PhotoView(
        val key: String,
        val url: String,
    )
}
