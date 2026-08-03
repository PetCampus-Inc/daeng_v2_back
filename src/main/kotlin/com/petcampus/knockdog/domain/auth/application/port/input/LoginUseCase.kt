package com.petcampus.knockdog.domain.auth.application.port.input

interface LoginUseCase {
    fun login(command: LoginCommand): TokenPair
}

data class LoginCommand(
    val oidcToken: String,
)
