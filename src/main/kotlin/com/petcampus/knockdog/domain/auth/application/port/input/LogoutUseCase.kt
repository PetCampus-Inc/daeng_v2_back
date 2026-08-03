package com.petcampus.knockdog.domain.auth.application.port.input

interface LogoutUseCase {
    fun logout(command: LogoutCommand)
}

data class LogoutCommand(
    val refreshToken: String,
)
