package com.petcampus.knockdog.domain.auth.application.port.output

interface LoadRefreshTokenPort {
    fun findByToken(token: String): RefreshTokenRecord?
}
