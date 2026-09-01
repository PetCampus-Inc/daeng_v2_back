package com.petcampus.knockdog.domain.auth.adapter.inbound.web

import com.petcampus.knockdog.domain.auth.application.port.input.VerifyOidcCommand
import com.petcampus.knockdog.domain.auth.application.port.input.VerifyOidcUseCase
import com.petcampus.knockdog.domain.auth.domain.Provider
import com.petcampus.knockdog.domain.auth.domain.SocialUser
import com.petcampus.knockdog.domain.auth.domain.SocialUserStatus
import com.petcampus.knockdog.global.response.Response
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
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
    ): ResponseEntity<Response<VerifyOidcResponse>> {
        val result =
            verifyOidcUseCase.verify(
                VerifyOidcCommand(request.provider, request.idToken, request.name, request.picture),
            )
        val cookie = authCookieFactory.oidcAuthCookie(result.oidcToken)
        val resultCode = VerifyOidcResultCode.from(result.socialUser.status)

        return ResponseEntity
            .ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(
                Response(
                    status = HttpStatus.OK.value(),
                    code = resultCode.code,
                    message = resultCode.message,
                    data = VerifyOidcResponse.from(result.socialUser),
                ),
            )
    }
}

data class VerifyOidcRequest(
    val provider: Provider,
    val idToken: String,
    val name: String?,
    val picture: String?,
)

data class VerifyOidcResponse(
    val provider: Provider,
    val name: String?,
    val picture: String?,
    val email: String,
) {
    companion object {
        fun from(socialUser: SocialUser): VerifyOidcResponse =
            VerifyOidcResponse(socialUser.provider, socialUser.name, socialUser.picture, socialUser.email)
    }
}

/** 레거시 VerifyOidcResultCode와 동일한 code/message 유지 — 프론트가 Response 봉투의 최상위 `code`로 로그인/가입/재연동을 분기한다. */
enum class VerifyOidcResultCode(
    val code: String,
    val message: String,
) {
    SUCCESS("SUCCESS", "정상적으로 처리되었습니다."),
    UNLINKED("UNLINKED", "회원 연결되지 않은 소셜 계정입니다."),
    EMAIL_ALREADY_EXISTS("EMAIL_ALREADY_EXISTS", "다른 소셜 계정으로 연결된 이메일입니다."),
    ;

    companion object {
        fun from(status: SocialUserStatus): VerifyOidcResultCode =
            when (status) {
                SocialUserStatus.LINKED -> SUCCESS
                SocialUserStatus.UNLINKED -> UNLINKED
                SocialUserStatus.PENDING -> EMAIL_ALREADY_EXISTS
            }
    }
}
