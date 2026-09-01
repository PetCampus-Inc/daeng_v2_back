package com.petcampus.knockdog.domain.auth.adapter.outbound.cache

import com.petcampus.knockdog.domain.auth.application.port.output.RefreshTokenRecord
import com.petcampus.knockdog.domain.auth.domain.UserCode

fun RefreshTokenRecord.toEntity(): RedisRefreshTokenEntity = RedisRefreshTokenEntity(token, userCode.value, ttlSeconds)

fun RedisRefreshTokenEntity.toRecord(): RefreshTokenRecord = RefreshTokenRecord(token, UserCode(userCode), ttlSeconds)
