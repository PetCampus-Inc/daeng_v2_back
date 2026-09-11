package com.petcampus.knockdog.domain.comparison.adapter.inbound.web

import com.petcampus.knockdog.domain.comparison.application.port.input.DeleteComparisonHistoryUseCase
import com.petcampus.knockdog.domain.comparison.application.port.input.GetComparisonHistoriesUseCase
import com.petcampus.knockdog.global.response.Response
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/kindergartens/comparisons/history")
class ComparisonHistoryController(
    private val getComparisonHistoriesUseCase: GetComparisonHistoriesUseCase,
    private val deleteComparisonHistoryUseCase: DeleteComparisonHistoryUseCase,
) {
    @GetMapping
    fun list(
        @AuthenticationPrincipal userCode: String,
        @RequestParam(defaultValue = "10") limit: Int,
    ): Response<List<ComparisonHistoryResponse>> =
        Response.success(
            getComparisonHistoriesUseCase.list(userCode, limit).map { ComparisonHistoryResponse.from(it) },
        )

    @DeleteMapping("/{historyId}")
    fun delete(
        @AuthenticationPrincipal userCode: String,
        @PathVariable historyId: Long,
    ): Response<Unit> {
        deleteComparisonHistoryUseCase.delete(userCode, historyId)
        return Response.success()
    }
}
