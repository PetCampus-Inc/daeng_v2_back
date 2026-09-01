package com.petcampus.knockdog.domain.auth.adapter.outbound.cache

import org.springframework.data.repository.CrudRepository

interface RedisRefreshTokenRepository : CrudRepository<RedisRefreshTokenEntity, String> {
    fun findAllByUserCode(userCode: String): List<RedisRefreshTokenEntity>
}
