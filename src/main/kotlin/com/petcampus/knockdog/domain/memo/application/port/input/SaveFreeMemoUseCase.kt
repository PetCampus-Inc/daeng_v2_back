package com.petcampus.knockdog.domain.memo.application.port.input

interface SaveFreeMemoUseCase {
    fun save(command: SaveFreeMemoCommand): FreeMemoView
}

data class SaveFreeMemoCommand(
    val userCode: String,
    val targetId: String,
    val content: String?,
    val photoKeys: List<String>,
)
