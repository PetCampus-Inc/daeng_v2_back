package com.petcampus.knockdog.domain.auth.application.port.input

interface RefreshTokenUseCase {
    fun refresh(command: RefreshTokenCommand): TokenPair
}

data class RefreshTokenCommand(
    val refreshToken: String,
)
