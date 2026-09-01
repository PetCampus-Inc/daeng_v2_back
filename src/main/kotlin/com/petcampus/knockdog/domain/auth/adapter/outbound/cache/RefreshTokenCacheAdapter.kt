package com.petcampus.knockdog.domain.auth.adapter.outbound.cache

import com.petcampus.knockdog.domain.auth.application.port.output.DeleteRefreshTokenPort
import com.petcampus.knockdog.domain.auth.application.port.output.LoadRefreshTokenPort
import com.petcampus.knockdog.domain.auth.application.port.output.RefreshTokenRecord
import com.petcampus.knockdog.domain.auth.application.port.output.SaveRefreshTokenPort
import com.petcampus.knockdog.domain.auth.domain.UserCode
import org.springframework.stereotype.Component

@Component
class RefreshTokenCacheAdapter(
    private val redisRefreshTokenRepository: RedisRefreshTokenRepository,
) : LoadRefreshTokenPort,
    SaveRefreshTokenPort,
    DeleteRefreshTokenPort {
    override fun findByToken(token: String): RefreshTokenRecord? =
        redisRefreshTokenRepository.findById(token).map { it.toRecord() }.orElse(null)

    override fun save(record: RefreshTokenRecord) {
        redisRefreshTokenRepository.save(record.toEntity())
    }

    override fun deleteByToken(token: String) {
        redisRefreshTokenRepository.deleteById(token)
    }

    override fun deleteAllByUserCode(userCode: UserCode) {
        redisRefreshTokenRepository.findAllByUserCode(userCode.value).forEach {
            redisRefreshTokenRepository.deleteById(it.token)
        }
    }
}
