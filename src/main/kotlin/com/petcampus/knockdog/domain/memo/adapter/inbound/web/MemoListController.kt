package com.petcampus.knockdog.domain.memo.adapter.inbound.web

import com.petcampus.knockdog.domain.memo.application.port.input.GetMemoedKindergartensUseCase
import com.petcampus.knockdog.global.response.Response
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/memos")
class MemoListController(
    private val getMemoedKindergartensUseCase: GetMemoedKindergartensUseCase,
) {
    @GetMapping
    fun list(
        @AuthenticationPrincipal userCode: String,
    ): Response<MemoListResponse> {
        val memos =
            getMemoedKindergartensUseCase.list(userCode).map {
                MemoListResponse.Item(shopId = it.shopId, content = it.content, memoDate = it.memoDate)
            }
        return Response.success(MemoListResponse(memos))
    }
}

data class MemoListResponse(
    val memos: List<Item>,
) {
    data class Item(
        val shopId: String,
        val content: String?,
        val memoDate: LocalDate,
    )
}
