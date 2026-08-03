package com.petcampus.knockdog.domain.auth.application.service

import com.petcampus.knockdog.domain.auth.application.AuthException
import com.petcampus.knockdog.domain.auth.application.port.input.RegisterUserCommand
import com.petcampus.knockdog.domain.auth.application.port.input.RegisterUserResult
import com.petcampus.knockdog.domain.auth.application.port.input.RegisterUserUseCase
import com.petcampus.knockdog.domain.auth.application.port.output.LoadSocialUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.SaveSocialUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.SaveUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.TokenPort
import com.petcampus.knockdog.domain.auth.domain.User
import com.petcampus.knockdog.domain.auth.domain.UserAddress
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
                ?: throw AuthException.notFoundSocialUser()

        if (socialUser.isLinked) {
            throw AuthException.alreadyLinkedUser()
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
