package com.petcampus.knockdog.domain.auth.application.service

import com.petcampus.knockdog.domain.auth.application.AuthErrorCode
import com.petcampus.knockdog.domain.auth.application.port.input.RefreshTokenCommand
import com.petcampus.knockdog.domain.auth.application.port.input.RefreshTokenUseCase
import com.petcampus.knockdog.domain.auth.application.port.input.TokenPair
import com.petcampus.knockdog.domain.auth.application.port.output.DeleteRefreshTokenPort
import com.petcampus.knockdog.domain.auth.application.port.output.LoadRefreshTokenPort
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Redis에 저장된 토큰 존재 여부만으로 검증한다(레거시 TokenService 동일 — 리프레시 토큰 자체의 서명 재검증은 하지 않음). */
@Service
class RefreshTokenService(
    private val loadRefreshTokenPort: LoadRefreshTokenPort,
    private val deleteRefreshTokenPort: DeleteRefreshTokenPort,
    private val tokenIssuer: TokenIssuer,
) : RefreshTokenUseCase {
    @Transactional
    override fun refresh(command: RefreshTokenCommand): TokenPair {
        val record =
            loadRefreshTokenPort.findByToken(command.refreshToken)
                ?: throw BusinessException(AuthErrorCode.INVALID_TOKEN)

        deleteRefreshTokenPort.deleteByToken(command.refreshToken)

        return tokenIssuer.issue(record.userCode)
    }
}
