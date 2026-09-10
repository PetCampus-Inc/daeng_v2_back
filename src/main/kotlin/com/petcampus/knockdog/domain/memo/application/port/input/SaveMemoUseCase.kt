package com.petcampus.knockdog.domain.memo.application.port.input

interface SaveMemoUseCase {
    fun save(command: SaveMemoCommand): MemoView
}

data class SaveMemoCommand(
    val userCode: String,
    val targetId: String,
    val content: String?,
)
