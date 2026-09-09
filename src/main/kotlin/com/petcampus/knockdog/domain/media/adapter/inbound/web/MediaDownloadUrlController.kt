package com.petcampus.knockdog.domain.media.adapter.inbound.web

import com.petcampus.knockdog.domain.media.application.port.input.IssueDownloadUrlCommand
import com.petcampus.knockdog.domain.media.application.port.input.IssueDownloadUrlUseCase
import com.petcampus.knockdog.global.response.Response
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/media")
class MediaDownloadUrlController(
    private val issueDownloadUrlUseCase: IssueDownloadUrlUseCase,
) {
    @PostMapping("/download-urls")
    fun issueDownloadUrl(
        @RequestBody request: DownloadUrlRequest,
    ): Response<DownloadUrlResponse> {
        val result = issueDownloadUrlUseCase.issue(IssueDownloadUrlCommand(key = request.key))

        return Response.success(DownloadUrlResponse(url = result.url, expiresIn = result.expiresIn))
    }
}

data class DownloadUrlRequest(
    val key: String,
)

data class DownloadUrlResponse(
    val url: String,
    val expiresIn: Long,
)
