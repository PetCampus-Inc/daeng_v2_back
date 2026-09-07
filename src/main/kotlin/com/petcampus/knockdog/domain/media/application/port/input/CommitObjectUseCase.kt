package com.petcampus.knockdog.domain.media.application.port.input

interface CommitObjectUseCase {
    fun commit(command: CommitObjectCommand): CommittedObject
}

data class CommitObjectCommand(
    val userCode: String,
    val key: String,
    val targetPath: String,
)

data class CommittedObject(
    val key: String,
    val url: String,
)
