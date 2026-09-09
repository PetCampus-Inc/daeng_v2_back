package com.petcampus.knockdog.domain.memo.adapter.inbound.web

import com.petcampus.knockdog.domain.memo.application.port.input.FreeMemoView
import com.petcampus.knockdog.domain.memo.application.port.input.GetFreeMemoUseCase
import com.petcampus.knockdog.domain.memo.application.port.input.SaveFreeMemoCommand
import com.petcampus.knockdog.domain.memo.application.port.input.SaveFreeMemoUseCase
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
class FreeMemoController(
    private val getFreeMemoUseCase: GetFreeMemoUseCase,
    private val saveFreeMemoUseCase: SaveFreeMemoUseCase,
) {
    @GetMapping("/{targetId}")
    fun get(
        @AuthenticationPrincipal userCode: String,
        @PathVariable targetId: String,
    ): Response<FreeMemoResponse> = Response.success(getFreeMemoUseCase.get(userCode, targetId).toResponse())

    @PutMapping("/{targetId}")
    fun save(
        @AuthenticationPrincipal userCode: String,
        @PathVariable targetId: String,
        @RequestBody request: SaveMemoRequest,
    ): Response<FreeMemoResponse> {
        val view =
            saveFreeMemoUseCase.save(
                SaveFreeMemoCommand(
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

data class FreeMemoResponse(
    val content: String?,
    val photos: List<PhotoResponse>,
) {
    data class PhotoResponse(
        val key: String,
        val url: String,
    )
}

private fun FreeMemoView.toResponse() =
    FreeMemoResponse(
        content = content,
        photos = photos.map { FreeMemoResponse.PhotoResponse(it.key, it.url) },
    )
