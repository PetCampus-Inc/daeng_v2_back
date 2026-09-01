package com.petcampus.knockdog.domain.auth.application.port.input

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)
