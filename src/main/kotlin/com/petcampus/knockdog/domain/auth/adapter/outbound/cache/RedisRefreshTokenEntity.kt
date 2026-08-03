package com.petcampus.knockdog.domain.auth.adapter.outbound.cache

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.TimeToLive
import org.springframework.data.redis.core.index.Indexed

@RedisHash("refresh_token")
class RedisRefreshTokenEntity(
    @Id
    val token: String,
    @Indexed
    val userCode: String,
    @TimeToLive
    val ttlSeconds: Long,
)
