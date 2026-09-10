package com.petcampus.knockdog.domain.memo.application.port.input

interface DeleteMemoPhotoUseCase {
    fun delete(command: DeleteMemoPhotoCommand)
}

data class DeleteMemoPhotoCommand(
    val userCode: String,
    val targetId: String,
    val photoId: Long,
)
