package com.petcampus.knockdog.domain.auth.adapter.inbound.web

import com.petcampus.knockdog.domain.auth.application.port.input.VerifyOidcCommand
import com.petcampus.knockdog.domain.auth.application.port.input.VerifyOidcUseCase
import com.petcampus.knockdog.domain.auth.domain.Provider
import com.petcampus.knockdog.domain.auth.domain.SocialUser
import com.petcampus.knockdog.domain.auth.domain.SocialUserStatus
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth/oidc-verifications")
class OidcVerificationController(
    private val verifyOidcUseCase: VerifyOidcUseCase,
    private val authCookieFactory: AuthCookieFactory,
) {
    @PostMapping
    fun verify(
        @RequestBody request: VerifyOidcRequest,
    ): ResponseEntity<VerifyOidcResponse> {
        val result =
            verifyOidcUseCase.verify(
                VerifyOidcCommand(request.provider, request.idToken, request.name, request.picture),
            )
        val cookie = authCookieFactory.oidcAuthCookie(result.oidcToken)

        return ResponseEntity
            .ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(VerifyOidcResponse.from(result.socialUser))
    }
}

data class VerifyOidcRequest(
    val provider: Provider,
    val idToken: String,
    val name: String?,
    val picture: String?,
)

data class VerifyOidcResponse(
    val status: String,
    val email: String,
) {
    companion object {
        // 레거시 VerifyOidcResultCode와 동일한 문자열 유지 — 프론트가 이 값으로 로그인/가입/재연동 분기를 한다
        // (docs/architecture/common-response-error.md §2, LINKED->SUCCESS, PENDING->EMAIL_ALREADY_EXISTS).
        fun from(socialUser: SocialUser): VerifyOidcResponse {
            val status =
                when (socialUser.status) {
                    SocialUserStatus.LINKED -> "SUCCESS"
                    SocialUserStatus.UNLINKED -> "UNLINKED"
                    SocialUserStatus.PENDING -> "EMAIL_ALREADY_EXISTS"
                }
            return VerifyOidcResponse(status, socialUser.email)
        }
    }
}
