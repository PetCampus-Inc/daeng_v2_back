package com.petcampus.knockdog.domain.auth.application.service

import com.petcampus.knockdog.domain.auth.application.AuthErrorCode
import com.petcampus.knockdog.domain.auth.application.port.input.RegisterUserCommand
import com.petcampus.knockdog.domain.auth.application.port.input.RegisterUserResult
import com.petcampus.knockdog.domain.auth.application.port.input.RegisterUserUseCase
import com.petcampus.knockdog.domain.auth.application.port.output.LoadSocialUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.SaveSocialUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.SaveUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.TokenPort
import com.petcampus.knockdog.domain.auth.domain.SocialUserStatus
import com.petcampus.knockdog.domain.auth.domain.User
import com.petcampus.knockdog.domain.auth.domain.UserAddress
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterUserService(
    private val tokenPort: TokenPort,
    private val loadSocialUserPort: LoadSocialUserPort,
    private val saveSocialUserPort: SaveSocialUserPort,
    private val saveUserPort: SaveUserPort,
    private val tokenIssuer: TokenIssuer,
) : RegisterUserUseCase {
    @Transactional
    override fun register(command: RegisterUserCommand): RegisterUserResult {
        val claims = tokenPort.parseOidcToken(command.oidcToken)
        val socialUser =
            loadSocialUserPort.findByProviderAndProviderId(claims.provider, claims.providerId)
                ?: throw BusinessException(AuthErrorCode.NOT_FOUND_SOCIAL_USER)

        // PENDING: 다른 provider가 같은 이메일로 이미 LINKED 상태 — 신규 가입이 아니라 재연동(A-4, 이번 범위 밖) 대상이다.
        // 여기서 막지 않으면 이 확인 절차 자체가 우회된다(VerifyOidcService의 PENDING 판정 의미가 사라짐).
        when (socialUser.status) {
            SocialUserStatus.LINKED -> throw BusinessException(AuthErrorCode.ALREADY_LINKED_USER)
            SocialUserStatus.PENDING -> throw BusinessException(AuthErrorCode.PENDING_SOCIAL_USER)
            SocialUserStatus.UNLINKED -> Unit
        }

        val addresses =
            command.addresses.map {
                UserAddress.create(it.type, it.alias, it.address, it.roadAddress, it.lat, it.lng)
            }

        val user =
            User.create(
                nickname = command.nickname,
                profileImage = command.profileImage,
                infoReceiveEmail = command.infoReceiveEmail,
                addresses = addresses,
            )
        val savedUser = saveUserPort.save(user)

        socialUser.link(requireNotNull(savedUser.id))
        saveSocialUserPort.save(socialUser)

        return RegisterUserResult(savedUser, tokenIssuer.issue(savedUser.code))
    }
}
