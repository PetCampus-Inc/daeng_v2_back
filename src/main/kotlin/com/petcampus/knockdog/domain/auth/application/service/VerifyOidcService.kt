package com.petcampus.knockdog.domain.auth.application.service

import com.petcampus.knockdog.domain.auth.application.port.input.VerifyOidcCommand
import com.petcampus.knockdog.domain.auth.application.port.input.VerifyOidcResult
import com.petcampus.knockdog.domain.auth.application.port.input.VerifyOidcUseCase
import com.petcampus.knockdog.domain.auth.application.port.output.LoadSocialUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.OidcIdentity
import com.petcampus.knockdog.domain.auth.application.port.output.OidcVerifierPort
import com.petcampus.knockdog.domain.auth.application.port.output.SaveSocialUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.TokenPort
import com.petcampus.knockdog.domain.auth.domain.Provider
import com.petcampus.knockdog.domain.auth.domain.SocialUser
import com.petcampus.knockdog.domain.auth.domain.SocialUserStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 레거시 VerifyOidcService 로직을 그대로 포팅한다 (ADR 0006).
 * (provider, providerId) 매치가 있으면 상태만 판정하고, 없으면 동일 이메일의 다른 provider 존재 여부로
 * PENDING/UNLINKED를 판정한 뒤 새 SocialUser row를 저장한다 — email에는 UNIQUE 제약을 걸지 않는다.
 */
@Service
class VerifyOidcService(
    private val oidcVerifierPort: OidcVerifierPort,
    private val loadSocialUserPort: LoadSocialUserPort,
    private val saveSocialUserPort: SaveSocialUserPort,
    private val tokenPort: TokenPort,
) : VerifyOidcUseCase {
    @Transactional
    override fun verify(command: VerifyOidcCommand): VerifyOidcResult {
        val identity = oidcVerifierPort.verify(command.provider, command.idToken)

        val socialUser =
            loadSocialUserPort.findByProviderAndProviderId(identity.provider, identity.providerId)
                ?: createNewSocialUser(identity, command)

        val oidcToken = tokenPort.issueOidcToken(socialUser.provider, socialUser.providerId, socialUser.email)

        return VerifyOidcResult(socialUser, oidcToken)
    }

    private fun createNewSocialUser(
        identity: OidcIdentity,
        command: VerifyOidcCommand,
    ): SocialUser {
        val hasDifferentProviderWithSameEmail =
            loadSocialUserPort.findAllByEmail(identity.email).any { it.provider != identity.provider }

        val status = if (hasDifferentProviderWithSameEmail) SocialUserStatus.PENDING else SocialUserStatus.UNLINKED

        val newSocialUser =
            SocialUser.create(
                provider = identity.provider,
                providerId = identity.providerId,
                email = identity.email,
                name = resolveName(command.name, identity.email, identity.provider),
                picture = command.picture,
                status = status,
            )

        return saveSocialUserPort.save(newSocialUser)
    }

    private fun resolveName(
        requestedName: String?,
        email: String,
        provider: Provider,
    ): String {
        if (!requestedName.isNullOrBlank()) return requestedName
        if (email.contains("@")) return email.substringBefore("@")
        return "${provider.name} 사용자"
    }
}
