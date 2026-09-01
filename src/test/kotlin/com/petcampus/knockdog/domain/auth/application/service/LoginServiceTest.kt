package com.petcampus.knockdog.domain.auth.application.service

import com.petcampus.knockdog.domain.auth.application.port.input.LoginCommand
import com.petcampus.knockdog.domain.auth.application.port.output.DeleteRefreshTokenPort
import com.petcampus.knockdog.domain.auth.application.port.output.LoadSocialUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.OidcTokenClaims
import com.petcampus.knockdog.domain.auth.application.port.output.RefreshTokenRecord
import com.petcampus.knockdog.domain.auth.application.port.output.SaveRefreshTokenPort
import com.petcampus.knockdog.domain.auth.application.port.output.SaveSocialUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.TokenPort
import com.petcampus.knockdog.domain.auth.domain.AddressType
import com.petcampus.knockdog.domain.auth.domain.Provider
import com.petcampus.knockdog.domain.auth.domain.SocialUser
import com.petcampus.knockdog.domain.auth.domain.SocialUserId
import com.petcampus.knockdog.domain.auth.domain.SocialUserStatus
import com.petcampus.knockdog.domain.auth.domain.User
import com.petcampus.knockdog.domain.auth.domain.UserAddress
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.auth.domain.UserId
import com.petcampus.knockdog.global.exception.BusinessException
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class LoginServiceTest {
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
        var socialUser: SocialUser?,
    ) : LoadSocialUserPort,
        SaveSocialUserPort {
        var saved: SocialUser? = null

        override fun findByProviderAndProviderId(
            provider: Provider,
            providerId: String,
        ): SocialUser? = socialUser

        override fun findAllByEmail(email: String): List<SocialUser> = listOfNotNull(socialUser)

        override fun save(socialUser: SocialUser): SocialUser {
            saved = socialUser
            return socialUser
        }
    }

    private class FixedUserPort(
        private val user: User?,
    ) : LoadUserPort {
        override fun findById(id: UserId) = user

        override fun findByCode(code: UserCode) = user
    }

    private val claims = OidcTokenClaims(Provider.GOOGLE, "g-1", "a@b.com")

    private fun loginService(
        socialUserPort: FixedSocialUserPort,
        userPort: FixedUserPort,
    ) = LoginService(
        StubTokenPort(claims),
        socialUserPort,
        socialUserPort,
        userPort,
        TokenIssuer(StubTokenPort(claims), NoopRefreshTokenPort(), NoopRefreshTokenPort(), 2_592_000_000L),
    )

    private fun activeUser(): User {
        val home = UserAddress.create(AddressType.HOME, null, "서울시", null, 37.5, 127.0)
        return User.reconstitute(UserId(1L), UserCode("ABCD1234"), "홍길동", null, null, null, null, null, listOf(home), null)
    }

    @Test
    fun `연동된 소셜 계정으로 활성 회원이면 토큰을 발급한다`() {
        val socialUser =
            SocialUser.reconstitute(
                SocialUserId(1L),
                Provider.GOOGLE,
                "g-1",
                "a@b.com",
                "홍길동",
                null,
                SocialUserStatus.LINKED,
                UserId(1L),
                null,
            )
        val service = loginService(FixedSocialUserPort(socialUser), FixedUserPort(activeUser()))

        val result = service.login(LoginCommand("temp-token"))

        assertEquals("access-token", result.tokenPair.accessToken)
    }

    @Test
    fun `소셜 계정을 찾을 수 없으면 예외가 발생한다`() {
        val service = loginService(FixedSocialUserPort(null), FixedUserPort(null))

        assertFailsWith<BusinessException> {
            service.login(LoginCommand("temp-token"))
        }
    }

    @Test
    fun `연동되지 않은 소셜 계정이면 예외가 발생한다`() {
        val socialUser =
            SocialUser.reconstitute(
                SocialUserId(1L),
                Provider.GOOGLE,
                "g-1",
                "a@b.com",
                "홍길동",
                null,
                SocialUserStatus.UNLINKED,
                null,
                null,
            )
        val service = loginService(FixedSocialUserPort(socialUser), FixedUserPort(null))

        assertFailsWith<BusinessException> {
            service.login(LoginCommand("temp-token"))
        }
    }

    @Test
    fun `탈퇴 후 7일 이내 회원은 재가입 제한 예외가 발생한다`() {
        val withdrawnUser =
            User.reconstitute(
                UserId(1L),
                UserCode("ABCD1234"),
                "홍길동",
                null,
                null,
                null,
                null,
                null,
                listOf(UserAddress.create(AddressType.HOME, null, "서울시", null, 37.5, 127.0)),
                LocalDateTime.now().minusDays(1),
            )
        val socialUser =
            SocialUser.reconstitute(
                SocialUserId(1L),
                Provider.GOOGLE,
                "g-1",
                "a@b.com",
                "홍길동",
                null,
                SocialUserStatus.LINKED,
                UserId(1L),
                null,
            )
        val socialUserPort = FixedSocialUserPort(socialUser)
        val service = loginService(socialUserPort, FixedUserPort(withdrawnUser))

        assertFailsWith<BusinessException> {
            service.login(LoginCommand("temp-token"))
        }
        assertEquals(null, socialUserPort.saved)
    }

    @Test
    fun `탈퇴 후 7일이 지난 회원은 소셜 계정 연동이 해제되고 예외가 발생한다`() {
        val withdrawnUser =
            User.reconstitute(
                UserId(1L),
                UserCode("ABCD1234"),
                "홍길동",
                null,
                null,
                null,
                null,
                null,
                listOf(UserAddress.create(AddressType.HOME, null, "서울시", null, 37.5, 127.0)),
                LocalDateTime.now().minusDays(8),
            )
        val socialUser =
            SocialUser.reconstitute(
                SocialUserId(1L),
                Provider.GOOGLE,
                "g-1",
                "a@b.com",
                "홍길동",
                null,
                SocialUserStatus.LINKED,
                UserId(1L),
                null,
            )
        val socialUserPort = FixedSocialUserPort(socialUser)
        val service = loginService(socialUserPort, FixedUserPort(withdrawnUser))

        assertFailsWith<BusinessException> {
            service.login(LoginCommand("temp-token"))
        }
        assertFalse(requireNotNull(socialUserPort.saved).isLinked)
    }
}
