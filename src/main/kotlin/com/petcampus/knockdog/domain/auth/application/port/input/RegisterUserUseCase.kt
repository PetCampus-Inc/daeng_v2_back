package com.petcampus.knockdog.domain.auth.application.port.input

import com.petcampus.knockdog.domain.auth.domain.AddressType
import com.petcampus.knockdog.domain.auth.domain.User

interface RegisterUserUseCase {
    fun register(command: RegisterUserCommand): RegisterUserResult
}

data class RegisterUserCommand(
    val oidcToken: String,
    val nickname: String?,
    val profileImage: String?,
    val infoReceiveEmail: String?,
    val addresses: List<RegisterAddressCommand>,
)

data class RegisterAddressCommand(
    val type: AddressType,
    val alias: String?,
    val address: String,
    val roadAddress: String?,
    val lat: Double,
    val lng: Double,
)

/** 레거시와 동일하게 회원가입 성공 시 즉시 로그인 상태가 된다(액세스/리프레시 토큰 발급). */
data class RegisterUserResult(
    val user: User,
    val tokenPair: TokenPair,
)
