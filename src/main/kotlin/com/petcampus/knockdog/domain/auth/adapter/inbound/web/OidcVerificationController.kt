package com.petcampus.knockdog.domain.auth.adapter.inbound.web

import com.petcampus.knockdog.domain.auth.application.port.input.VerifyOidcCommand
import com.petcampus.knockdog.domain.auth.application.port.input.VerifyOidcUseCase
import com.petcampus.knockdog.domain.auth.domain.Provider
import com.petcampus.knockdog.domain.auth.domain.SocialUser
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
        fun from(socialUser: SocialUser): VerifyOidcResponse = VerifyOidcResponse(socialUser.status.name, socialUser.email)
    }
}
