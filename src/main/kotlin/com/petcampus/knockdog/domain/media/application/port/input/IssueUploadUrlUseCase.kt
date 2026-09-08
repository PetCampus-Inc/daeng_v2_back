package com.petcampus.knockdog.domain.media.application.port.input

interface IssueUploadUrlUseCase {
    fun issue(command: IssueUploadUrlCommand): UploadUrl
}

data class IssueUploadUrlCommand(
    val userCode: String,
    val purpose: String,
    val contentType: String,
)

data class UploadUrl(
    val url: String,
    val key: String,
    val expiresIn: Long,
)
