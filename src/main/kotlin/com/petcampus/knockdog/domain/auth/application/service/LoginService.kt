package com.petcampus.knockdog.domain.auth.application.service

import com.petcampus.knockdog.domain.auth.application.AuthErrorCode
import com.petcampus.knockdog.domain.auth.application.port.input.LoginCommand
import com.petcampus.knockdog.domain.auth.application.port.input.LoginResult
import com.petcampus.knockdog.domain.auth.application.port.input.LoginUseCase
import com.petcampus.knockdog.domain.auth.application.port.output.LoadSocialUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.SaveSocialUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.TokenPort
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class LoginService(
    private val tokenPort: TokenPort,
    private val loadSocialUserPort: LoadSocialUserPort,
    private val saveSocialUserPort: SaveSocialUserPort,
    private val loadUserPort: LoadUserPort,
    private val tokenIssuer: TokenIssuer,
) : LoginUseCase {
    @Transactional
    override fun login(command: LoginCommand): LoginResult {
        val claims = tokenPort.parseOidcToken(command.oidcToken)
        val socialUser =
            loadSocialUserPort.findByProviderAndProviderId(claims.provider, claims.providerId)
                ?: throw BusinessException(AuthErrorCode.NOT_FOUND_SOCIAL_USER)

        if (!socialUser.isLinked) {
            throw BusinessException(AuthErrorCode.USER_NOT_LINKED)
        }

        val user =
            loadUserPort.findById(requireNotNull(socialUser.userId))
                ?: throw BusinessException(AuthErrorCode.NOT_FOUND_USER)

        if (user.isWithdrawn) {
            val withdrawnAt = requireNotNull(user.deletedAt)
            if (withdrawnAt.plusDays(REJOINING_RESTRICTION_DAYS).isBefore(LocalDateTime.now())) {
                socialUser.unlink()
                saveSocialUserPort.save(socialUser)
                throw BusinessException(AuthErrorCode.WITHDRAWN_USER)
            }
            throw BusinessException(AuthErrorCode.REJOINING_RESTRICTION_PERIOD)
        }

        return LoginResult(user, tokenIssuer.issue(user.code))
    }

    companion object {
        private const val REJOINING_RESTRICTION_DAYS = 7L
    }
}
