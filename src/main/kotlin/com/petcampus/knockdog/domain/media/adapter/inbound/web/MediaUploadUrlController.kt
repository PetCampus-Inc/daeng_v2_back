package com.petcampus.knockdog.domain.media.adapter.inbound.web

import com.petcampus.knockdog.domain.media.application.port.input.IssueUploadUrlCommand
import com.petcampus.knockdog.domain.media.application.port.input.IssueUploadUrlUseCase
import com.petcampus.knockdog.global.response.Response
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/media")
class MediaUploadUrlController(
    private val issueUploadUrlUseCase: IssueUploadUrlUseCase,
) {
    @PostMapping("/upload-urls")
    fun issueUploadUrl(
        @AuthenticationPrincipal userCode: String,
        @RequestBody request: UploadUrlRequest,
    ): Response<UploadUrlResponse> {
        val result =
            issueUploadUrlUseCase.issue(
                IssueUploadUrlCommand(userCode = userCode, purpose = request.purpose, contentType = request.contentType),
            )

        return Response.success(UploadUrlResponse(url = result.url, key = result.key, expiresIn = result.expiresIn))
    }
}

data class UploadUrlRequest(
    val purpose: String,
    val contentType: String,
)

data class UploadUrlResponse(
    val url: String,
    val key: String,
    val expiresIn: Long,
)
