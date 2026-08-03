package com.petcampus.knockdog.domain.auth.application.service

import com.petcampus.knockdog.domain.auth.application.AuthException
import com.petcampus.knockdog.domain.auth.application.port.input.LoginCommand
import com.petcampus.knockdog.domain.auth.application.port.input.LoginUseCase
import com.petcampus.knockdog.domain.auth.application.port.input.TokenPair
import com.petcampus.knockdog.domain.auth.application.port.output.LoadSocialUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.SaveSocialUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.TokenPort
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
    override fun login(command: LoginCommand): TokenPair {
        val claims = tokenPort.parseOidcToken(command.oidcToken)
        val socialUser =
            loadSocialUserPort.findByProviderAndProviderId(claims.provider, claims.providerId)
                ?: throw AuthException.notFoundSocialUser()

        if (!socialUser.isLinked) {
            throw AuthException.userNotLinked()
        }

        val user =
            loadUserPort.findById(requireNotNull(socialUser.userId))
                ?: throw AuthException.notFoundUser()

        if (user.isWithdrawn) {
            val withdrawnAt = requireNotNull(user.deletedAt)
            if (withdrawnAt.plusDays(REJOINING_RESTRICTION_DAYS).isBefore(LocalDateTime.now())) {
                socialUser.unlink()
                saveSocialUserPort.save(socialUser)
                throw AuthException.withdrawnUser()
            }
            throw AuthException.rejoiningRestrictionPeriod()
        }

        return tokenIssuer.issue(user.code)
    }

    companion object {
        private const val REJOINING_RESTRICTION_DAYS = 7L
    }
}
