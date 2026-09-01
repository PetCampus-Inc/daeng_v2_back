package com.petcampus.knockdog.domain.auth.application.service

import com.petcampus.knockdog.domain.auth.application.port.input.TokenPair
import com.petcampus.knockdog.domain.auth.application.port.output.DeleteRefreshTokenPort
import com.petcampus.knockdog.domain.auth.application.port.output.RefreshTokenRecord
import com.petcampus.knockdog.domain.auth.application.port.output.SaveRefreshTokenPort
import com.petcampus.knockdog.domain.auth.application.port.output.TokenPort
import com.petcampus.knockdog.domain.auth.domain.UserCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/** 로그인/재발급/회원가입 3곳에서 공통으로 쓰는 "기존 리프레시 토큰 무효화 후 재발급" 절차. */
@Component
class TokenIssuer(
    private val tokenPort: TokenPort,
    private val saveRefreshTokenPort: SaveRefreshTokenPort,
    private val deleteRefreshTokenPort: DeleteRefreshTokenPort,
    @param:Value("\${jwt.token.durations.refresh}") private val refreshTokenDurationMs: Long,
) {
    fun issue(userCode: UserCode): TokenPair {
        deleteRefreshTokenPort.deleteAllByUserCode(userCode)

        val accessToken = tokenPort.issueAccessToken(userCode)
        val refreshToken = tokenPort.issueRefreshToken(userCode)

        saveRefreshTokenPort.save(RefreshTokenRecord(refreshToken, userCode, refreshTokenDurationMs / 1000))

        return TokenPair(accessToken, refreshToken)
    }
}
