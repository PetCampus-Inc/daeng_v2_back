package com.petcampus.knockdog.domain.auth.application.port.output

import com.petcampus.knockdog.domain.auth.domain.UserCode

/** Redis에 저장되는 리프레시 토큰 레코드. 만료는 Redis TTL이 책임진다. */
data class RefreshTokenRecord(
    val token: String,
    val userCode: UserCode,
    val ttlSeconds: Long,
)
