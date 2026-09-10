package com.petcampus.knockdog.domain.memo.application.port.input

interface AddMemoPhotoUseCase {
    fun add(command: AddMemoPhotoCommand): MemoView.PhotoView
}

data class AddMemoPhotoCommand(
    val userCode: String,
    val targetId: String,
    val photoKey: String,
)
