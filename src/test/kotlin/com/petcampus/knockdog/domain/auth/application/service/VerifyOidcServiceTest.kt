package com.petcampus.knockdog.domain.auth.application.service

import com.petcampus.knockdog.domain.auth.application.port.input.VerifyOidcCommand
import com.petcampus.knockdog.domain.auth.application.port.output.LoadSocialUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.OidcIdentity
import com.petcampus.knockdog.domain.auth.application.port.output.OidcVerifierPort
import com.petcampus.knockdog.domain.auth.application.port.output.SaveSocialUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.TokenPort
import com.petcampus.knockdog.domain.auth.domain.Provider
import com.petcampus.knockdog.domain.auth.domain.SocialUser
import com.petcampus.knockdog.domain.auth.domain.SocialUserId
import com.petcampus.knockdog.domain.auth.domain.SocialUserStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VerifyOidcServiceTest {
    private class FixedOidcVerifierPort(
        private val identity: OidcIdentity,
    ) : OidcVerifierPort {
        override fun verify(
            provider: Provider,
            idToken: String,
        ): OidcIdentity = identity
    }

    private class InMemorySocialUserStore :
        LoadSocialUserPort,
        SaveSocialUserPort {
        val all = mutableListOf<SocialUser>()
        private var nextId = 1L

        override fun findByProviderAndProviderId(
            provider: Provider,
            providerId: String,
        ): SocialUser? = all.find { it.provider == provider && it.providerId == providerId }

        override fun findAllByEmail(email: String): List<SocialUser> = all.filter { it.email == email }

        override fun save(socialUser: SocialUser): SocialUser {
            if (socialUser.id != null) return socialUser.also { all.add(it) }

            val saved =
                SocialUser.reconstitute(
                    id = SocialUserId(nextId++),
                    provider = socialUser.provider,
                    providerId = socialUser.providerId,
                    email = socialUser.email,
                    name = socialUser.name,
                    picture = socialUser.picture,
                    status = socialUser.status,
                    userId = socialUser.userId,
                    linkedAt = socialUser.linkedAt,
                )
            all.add(saved)
            return saved
        }
    }

    private class StubTokenPort : TokenPort {
        override fun issueOidcToken(
            provider: Provider,
            providerId: String,
            email: String,
        ) = "oidc-token"

        override fun parseOidcToken(token: String) = throw NotImplementedError()

        override fun issueAccessToken(userCode: com.petcampus.knockdog.domain.auth.domain.UserCode) = throw NotImplementedError()

        override fun issueRefreshToken(userCode: com.petcampus.knockdog.domain.auth.domain.UserCode) = throw NotImplementedError()

        override fun parseAccessTokenSubject(token: String) = throw NotImplementedError()
    }

    private fun service(
        identity: OidcIdentity,
        store: InMemorySocialUserStore,
    ) = VerifyOidcService(FixedOidcVerifierPort(identity), store, store, StubTokenPort())

    @Test
    fun `이미 LINKED인 소셜 계정이면 그 상태 그대로 반환한다`() {
        val store = InMemorySocialUserStore()
        store.all.add(
            SocialUser.reconstitute(
                SocialUserId(1L),
                Provider.GOOGLE,
                "g-1",
                "a@b.com",
                "홍길동",
                null,
                SocialUserStatus.LINKED,
                com.petcampus.knockdog.domain.auth.domain
                    .UserId(1L),
                null,
            ),
        )
        val svc = service(OidcIdentity(Provider.GOOGLE, "g-1", "a@b.com"), store)

        val result = svc.verify(VerifyOidcCommand(Provider.GOOGLE, "id-token", null, null))

        assertEquals(SocialUserStatus.LINKED, result.socialUser.status)
        assertEquals(1, store.all.size)
    }

    @Test
    fun `매치되는 provider+providerId가 없고 동일 이메일 다른 provider도 없으면 UNLINKED로 신규 생성한다`() {
        val store = InMemorySocialUserStore()
        val svc = service(OidcIdentity(Provider.GOOGLE, "g-1", "a@b.com"), store)

        val result = svc.verify(VerifyOidcCommand(Provider.GOOGLE, "id-token", "홍길동", null))

        assertEquals(SocialUserStatus.UNLINKED, result.socialUser.status)
        assertEquals(1, store.all.size)
    }

    @Test
    fun `매치되는 provider+providerId가 없고 동일 이메일의 다른 provider가 이미 있으면 PENDING으로 신규 생성한다`() {
        val store = InMemorySocialUserStore()
        store.all.add(
            SocialUser.reconstitute(
                SocialUserId(1L),
                Provider.KAKAO,
                "k-1",
                "a@b.com",
                "홍길동",
                null,
                SocialUserStatus.LINKED,
                com.petcampus.knockdog.domain.auth.domain
                    .UserId(1L),
                null,
            ),
        )
        val svc = service(OidcIdentity(Provider.GOOGLE, "g-1", "a@b.com"), store)

        val result = svc.verify(VerifyOidcCommand(Provider.GOOGLE, "id-token", "홍길동", null))

        assertEquals(SocialUserStatus.PENDING, result.socialUser.status)
        assertEquals(2, store.all.size)
    }
}
