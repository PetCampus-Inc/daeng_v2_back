package com.petcampus.knockdog.domain.auth.application.port.output

interface SaveRefreshTokenPort {
    fun save(record: RefreshTokenRecord)
}
