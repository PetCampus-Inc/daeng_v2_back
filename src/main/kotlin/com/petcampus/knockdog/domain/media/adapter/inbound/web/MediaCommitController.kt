package com.petcampus.knockdog.domain.media.adapter.inbound.web

import com.petcampus.knockdog.domain.media.application.port.input.CommitObjectCommand
import com.petcampus.knockdog.domain.media.application.port.input.CommitObjectUseCase
import com.petcampus.knockdog.global.response.Response
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/media")
class MediaCommitController(
    private val commitObjectUseCase: CommitObjectUseCase,
) {
    @PostMapping("/commits")
    fun commit(
        @AuthenticationPrincipal userCode: String,
        @RequestBody request: CommitRequest,
    ): Response<CommitResponse> {
        val result =
            commitObjectUseCase.commit(
                CommitObjectCommand(userCode = userCode, key = request.key, targetPath = request.targetPath),
            )

        return Response.success(CommitResponse(key = result.key, url = result.url))
    }
}

data class CommitRequest(
    val key: String,
    val targetPath: String,
)

data class CommitResponse(
    val key: String,
    val url: String,
)
