package com.petcampus.knockdog.domain.auth.application.port.input

import com.petcampus.knockdog.domain.auth.domain.User

interface LoginUseCase {
    fun login(command: LoginCommand): LoginResult
}

data class LoginCommand(
    val oidcToken: String,
)

/** 레거시와 동일하게 로그인 성공 응답 바디에도 유저 정보가 포함된다. */
data class LoginResult(
    val user: User,
    val tokenPair: TokenPair,
)
