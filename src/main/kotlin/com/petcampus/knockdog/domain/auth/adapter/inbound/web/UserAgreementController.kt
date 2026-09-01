package com.petcampus.knockdog.domain.auth.adapter.inbound.web

import com.petcampus.knockdog.domain.auth.application.port.input.AgreeToTermsCommand
import com.petcampus.knockdog.domain.auth.application.port.input.AgreeToTermsUseCase
import com.petcampus.knockdog.domain.auth.application.port.input.GetAgreementStatusUseCase
import com.petcampus.knockdog.domain.auth.domain.AgreementTermType
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.global.response.Response
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 약관 동의는 인벤토리에서 `KEEP`으로 판정된 v0 API다 — 프론트가 이미 이 경로를 호출 중이라
 * path/method/요청·응답 필드를 레거시 그대로 유지한다 (docs/rules/api-migration.md §2).
 * 다른 auth API처럼 v1으로 옮기지 않는 이유가 여기에 있다.
 */
@RestController
@RequestMapping("/api/v0/user")
class UserAgreementController(
    private val agreeToTermsUseCase: AgreeToTermsUseCase,
    private val getAgreementStatusUseCase: GetAgreementStatusUseCase,
) {
    @PostMapping("/agreements")
    fun agreeToTerms(
        @AuthenticationPrincipal userCode: String,
        @RequestBody request: AgreeToTermsRequest,
    ): ResponseEntity<Response<Unit>> {
        agreeToTermsUseCase.agree(AgreeToTermsCommand(UserCode(userCode), request.agreedTerms))

        return ResponseEntity.ok(Response.success())
    }

    @GetMapping("/agreements/status")
    fun getAgreementStatus(
        @AuthenticationPrincipal userCode: String,
    ): ResponseEntity<Response<AgreementStatusResponse>> {
        val hasAgreed = getAgreementStatusUseCase.hasAgreedRequiredTerms(UserCode(userCode))

        return ResponseEntity.ok(Response.success(AgreementStatusResponse(hasAgreed)))
    }
}

data class AgreeToTermsRequest(
    val agreedTerms: List<AgreementTermType>,
)

data class AgreementStatusResponse(
    val hasAgreedRequiredTerms: Boolean,
)
