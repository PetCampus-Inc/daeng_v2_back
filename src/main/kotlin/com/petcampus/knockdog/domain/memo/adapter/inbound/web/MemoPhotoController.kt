package com.petcampus.knockdog.domain.memo.adapter.inbound.web

import com.petcampus.knockdog.domain.memo.application.port.input.AddMemoPhotoCommand
import com.petcampus.knockdog.domain.memo.application.port.input.AddMemoPhotoUseCase
import com.petcampus.knockdog.domain.memo.application.port.input.DeleteMemoPhotoCommand
import com.petcampus.knockdog.domain.memo.application.port.input.DeleteMemoPhotoUseCase
import com.petcampus.knockdog.global.response.Response
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/memos/{targetId}/photos")
class MemoPhotoController(
    private val addMemoPhotoUseCase: AddMemoPhotoUseCase,
    private val deleteMemoPhotoUseCase: DeleteMemoPhotoUseCase,
) {
    @PostMapping
    fun add(
        @AuthenticationPrincipal userCode: String,
        @PathVariable targetId: String,
        @RequestBody request: AddMemoPhotoRequest,
    ): Response<MemoResponse.PhotoResponse> {
        val photo =
            addMemoPhotoUseCase.add(
                AddMemoPhotoCommand(userCode = userCode, targetId = targetId, photoKey = request.photoKey),
            )
        return Response.success(MemoResponse.PhotoResponse(id = photo.id, key = photo.key, url = photo.url))
    }

    @DeleteMapping("/{photoId}")
    fun delete(
        @AuthenticationPrincipal userCode: String,
        @PathVariable targetId: String,
        @PathVariable photoId: Long,
    ): Response<Unit> {
        deleteMemoPhotoUseCase.delete(
            DeleteMemoPhotoCommand(userCode = userCode, targetId = targetId, photoId = photoId),
        )
        return Response.success()
    }
}

data class AddMemoPhotoRequest(
    val photoKey: String,
)
