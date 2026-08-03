package com.petcampus.knockdog.domain.auth.application.service

import com.petcampus.knockdog.domain.auth.application.AuthException
import com.petcampus.knockdog.domain.auth.application.port.input.RegisterUserCommand
import com.petcampus.knockdog.domain.auth.application.port.output.DeleteRefreshTokenPort
import com.petcampus.knockdog.domain.auth.application.port.output.LoadSocialUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.OidcTokenClaims
import com.petcampus.knockdog.domain.auth.application.port.output.RefreshTokenRecord
import com.petcampus.knockdog.domain.auth.application.port.output.SaveRefreshTokenPort
import com.petcampus.knockdog.domain.auth.application.port.output.SaveSocialUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.SaveUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.TokenPort
import com.petcampus.knockdog.domain.auth.domain.AddressType
import com.petcampus.knockdog.domain.auth.domain.Provider
import com.petcampus.knockdog.domain.auth.domain.SocialUser
import com.petcampus.knockdog.domain.auth.domain.SocialUserId
import com.petcampus.knockdog.domain.auth.domain.SocialUserStatus
import com.petcampus.knockdog.domain.auth.domain.User
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.auth.domain.UserId
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class RegisterUserServiceTest {
    private class StubTokenPort(
        private val claims: OidcTokenClaims,
    ) : TokenPort {
        override fun issueOidcToken(
            provider: Provider,
            providerId: String,
            email: String,
        ) = throw NotImplementedError()

        override fun parseOidcToken(token: String) = claims

        override fun issueAccessToken(userCode: UserCode) = "access-token"

        override fun issueRefreshToken(userCode: UserCode) = "refresh-token"

        override fun parseAccessTokenSubject(token: String) = throw NotImplementedError()
    }

    private class NoopRefreshTokenPort :
        SaveRefreshTokenPort,
        DeleteRefreshTokenPort {
        override fun save(record: RefreshTokenRecord) = Unit

        override fun deleteByToken(token: String) = Unit

        override fun deleteAllByUserCode(userCode: UserCode) = Unit
    }

    private class FixedSocialUserPort(
        private val socialUser: SocialUser,
    ) : LoadSocialUserPort,
        SaveSocialUserPort {
        override fun findByProviderAndProviderId(
            provider: Provider,
            providerId: String,
        ) = socialUser

        override fun findAllByEmail(email: String) = listOf(socialUser)

        override fun save(socialUser: SocialUser) = socialUser
    }

    private class FixedSaveUserPort : SaveUserPort {
        override fun save(user: User): User =
            User.reconstitute(
                UserId(1L),
                user.code,
                user.nickname,
                user.profileImage,
                user.infoReceiveEmail,
                null,
                null,
                null,
                user.addresses,
                null,
            )
    }

    private val claims = OidcTokenClaims(Provider.GOOGLE, "g-1", "a@b.com")

    private fun service(socialUser: SocialUser) =
        RegisterUserService(
            StubTokenPort(claims),
            FixedSocialUserPort(socialUser),
            FixedSocialUserPort(socialUser),
            FixedSaveUserPort(),
            TokenIssuer(StubTokenPort(claims), NoopRefreshTokenPort(), NoopRefreshTokenPort(), 2_592_000_000L),
        )

    private fun command() =
        RegisterUserCommand(
            oidcToken = "oidc-token",
            nickname = "홍길동",
            profileImage = null,
            infoReceiveEmail = null,
            addresses =
                listOf(
                    com.petcampus.knockdog.domain.auth.application.port.input
                        .RegisterAddressCommand(AddressType.HOME, null, "서울시", null, 37.5, 127.0),
                ),
        )

    private fun socialUser(status: SocialUserStatus) =
        SocialUser.reconstitute(
            SocialUserId(1L),
            Provider.GOOGLE,
            "g-1",
            "a@b.com",
            "홍길동",
            null,
            status,
            if (status == SocialUserStatus.LINKED) UserId(1L) else null,
            null,
        )

    @Test
    fun `UNLINKED 상태면 회원가입에 성공한다`() {
        val result = service(socialUser(SocialUserStatus.UNLINKED)).register(command())

        kotlin.test.assertEquals("access-token", result.tokenPair.accessToken)
    }

    @Test
    fun `LINKED 상태(이미 연동됨)면 회원가입이 거부된다`() {
        assertFailsWith<AuthException> {
            service(socialUser(SocialUserStatus.LINKED)).register(command())
        }
    }

    @Test
    fun `PENDING 상태(다른 provider로 이미 가입된 이메일)면 회원가입이 거부된다`() {
        assertFailsWith<AuthException> {
            service(socialUser(SocialUserStatus.PENDING)).register(command())
        }
    }
}
