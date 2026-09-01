package com.petcampus.knockdog.domain.auth.adapter.inbound.web

import com.petcampus.knockdog.domain.auth.application.port.input.RegisterAddressCommand
import com.petcampus.knockdog.domain.auth.application.port.input.RegisterUserCommand
import com.petcampus.knockdog.domain.auth.application.port.input.RegisterUserUseCase
import com.petcampus.knockdog.domain.auth.domain.AddressType
import com.petcampus.knockdog.global.response.Response
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val registerUserUseCase: RegisterUserUseCase,
    private val authCookieFactory: AuthCookieFactory,
) {
    @PostMapping
    fun register(
        @CookieValue(AuthCookieFactory.OIDC_AUTH_COOKIE) oidcToken: String,
        @RequestBody request: RegisterUserRequest,
    ): ResponseEntity<Response<AuthUserResponse>> {
        val result =
            registerUserUseCase.register(
                RegisterUserCommand(
                    oidcToken = oidcToken,
                    nickname = request.nickname,
                    profileImage = request.profileImage,
                    infoReceiveEmail = request.infoReceiveEmail,
                    addresses = request.addresses.map { it.toCommand() },
                ),
            )

        val refreshCookie = authCookieFactory.refreshTokenCookie(result.tokenPair.refreshToken)
        val expiredOidcCookie = authCookieFactory.expiredOidcAuthCookie()

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + result.tokenPair.accessToken)
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .header(HttpHeaders.SET_COOKIE, expiredOidcCookie.toString())
            .body(Response.success(AuthUserResponse.from(result.user)))
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}

data class RegisterUserRequest(
    val nickname: String?,
    val profileImage: String?,
    val infoReceiveEmail: String?,
    val addresses: List<RegisterAddressRequest>,
)

data class RegisterAddressRequest(
    val type: AddressType,
    val alias: String?,
    val address: String,
    val roadAddress: String?,
    val lat: Double,
    val lng: Double,
) {
    fun toCommand(): RegisterAddressCommand = RegisterAddressCommand(type, alias, address, roadAddress, lat, lng)
}
