package com.petcampus.knockdog.domain.media.application.port.input

interface IssueDownloadUrlUseCase {
    fun issue(command: IssueDownloadUrlCommand): DownloadUrl
}

data class IssueDownloadUrlCommand(
    val key: String,
)

data class DownloadUrl(
    val url: String,
    val expiresIn: Long,
)
