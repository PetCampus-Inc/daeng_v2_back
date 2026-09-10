package com.petcampus.knockdog.domain.memo.adapter.inbound.web

import com.petcampus.knockdog.domain.memo.application.port.input.GetMemoUseCase
import com.petcampus.knockdog.domain.memo.application.port.input.MemoView
import com.petcampus.knockdog.domain.memo.application.port.input.SaveMemoCommand
import com.petcampus.knockdog.domain.memo.application.port.input.SaveMemoUseCase
import com.petcampus.knockdog.global.response.Response
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/memos")
class MemoController(
    private val getMemoUseCase: GetMemoUseCase,
    private val saveMemoUseCase: SaveMemoUseCase,
) {
    @GetMapping("/{targetId}")
    fun get(
        @AuthenticationPrincipal userCode: String,
        @PathVariable targetId: String,
    ): Response<MemoResponse> = Response.success(getMemoUseCase.get(userCode, targetId).toResponse())

    @PutMapping("/{targetId}")
    fun save(
        @AuthenticationPrincipal userCode: String,
        @PathVariable targetId: String,
        @RequestBody request: SaveMemoRequest,
    ): Response<MemoResponse> {
        val view =
            saveMemoUseCase.save(
                SaveMemoCommand(
                    userCode = userCode,
                    targetId = targetId,
                    content = request.content,
                    photoKeys = request.photoKeys ?: emptyList(),
                ),
            )
        return Response.success(view.toResponse())
    }
}

data class SaveMemoRequest(
    val content: String? = null,
    val photoKeys: List<String>? = null,
)

data class MemoResponse(
    val content: String?,
    val photos: List<PhotoResponse>,
) {
    data class PhotoResponse(
        val key: String,
        val url: String,
    )
}

private fun MemoView.toResponse() =
    MemoResponse(
        content = content,
        photos = photos.map { MemoResponse.PhotoResponse(it.key, it.url) },
    )
