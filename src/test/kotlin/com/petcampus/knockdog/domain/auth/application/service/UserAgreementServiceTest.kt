package com.petcampus.knockdog.domain.auth.application.service

import com.petcampus.knockdog.domain.auth.application.AuthErrorCode
import com.petcampus.knockdog.domain.auth.application.port.input.AgreeToTermsCommand
import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserAgreementPort
import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.SaveUserAgreementPort
import com.petcampus.knockdog.domain.auth.domain.AddressType
import com.petcampus.knockdog.domain.auth.domain.AgreementTermType
import com.petcampus.knockdog.domain.auth.domain.User
import com.petcampus.knockdog.domain.auth.domain.UserAddress
import com.petcampus.knockdog.domain.auth.domain.UserAgreement
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.auth.domain.UserId
import com.petcampus.knockdog.global.exception.BusinessException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserAgreementServiceTest {
    private val userCode = UserCode("abcd1234")
    private val userId = UserId(1L)

    private fun user() =
        User.reconstitute(
            id = userId,
            code = userCode,
            nickname = "홍길동",
            profileImage = null,
            infoReceiveEmail = null,
            gender = null,
            phoneNumber = null,
            emergencyPhoneNumber = null,
            addresses = listOf(UserAddress.create(AddressType.HOME, null, "서울시", null, 37.5, 127.0)),
            deletedAt = null,
        )

    private class StubLoadUserPort(
        private val user: User?,
    ) : LoadUserPort {
        override fun findById(id: UserId): User? = user

        override fun findByCode(code: UserCode): User? = user
    }

    private class FakeAgreementPort(
        initial: Set<AgreementTermType> = emptySet(),
    ) : LoadUserAgreementPort,
        SaveUserAgreementPort {
        val saved = mutableListOf<UserAgreement>()
        private val existing = initial.toMutableSet()

        override fun findTermTypesByUserId(userId: UserId): Set<AgreementTermType> = existing.toSet()

        override fun saveAll(agreements: List<UserAgreement>) {
            saved.addAll(agreements)
            agreements.forEach { existing.add(it.termType) }
        }
    }

    private fun service(
        loadUser: LoadUserPort = StubLoadUserPort(user()),
        agreements: FakeAgreementPort = FakeAgreementPort(),
    ) = UserAgreementService(loadUser, agreements, agreements)

    @Test
    fun `필수 약관에 모두 동의하면 저장된다`() {
        val port = FakeAgreementPort()

        service(agreements = port).agree(AgreeToTermsCommand(userCode, AgreementTermType.REQUIRED.toList()))

        assertEquals(AgreementTermType.REQUIRED, port.saved.map { it.termType }.toSet())
    }

    @Test
    fun `필수 약관이 하나라도 빠지면 REQUIRED_AGREEMENT_NOT_COMPLETED로 거부한다`() {
        val port = FakeAgreementPort()

        val exception =
            assertFailsWith<BusinessException> {
                service(agreements = port).agree(
                    AgreeToTermsCommand(
                        userCode,
                        listOf(AgreementTermType.TERMS_OF_SERVICE, AgreementTermType.PRIVACY_POLICY),
                    ),
                )
            }

        assertEquals(AuthErrorCode.REQUIRED_AGREEMENT_NOT_COMPLETED, exception.errorCode)
        assertTrue(port.saved.isEmpty())
    }

    /** 레거시는 insertIgnoringDuplicateKey로 중복을 무시한다. (user_id, term_type) unique를 위반하지 않아야 한다. */
    @Test
    fun `이미 동의한 약관은 다시 저장하지 않는다`() {
        val port = FakeAgreementPort(initial = setOf(AgreementTermType.TERMS_OF_SERVICE))

        service(agreements = port).agree(AgreeToTermsCommand(userCode, AgreementTermType.REQUIRED.toList()))

        assertEquals(
            setOf(AgreementTermType.PRIVACY_POLICY, AgreementTermType.AGE_OVER_14),
            port.saved.map { it.termType }.toSet(),
        )
    }

    @Test
    fun `존재하지 않는 회원이면 NOT_FOUND_USER로 거부한다`() {
        val exception =
            assertFailsWith<BusinessException> {
                service(loadUser = StubLoadUserPort(null)).agree(
                    AgreeToTermsCommand(userCode, AgreementTermType.REQUIRED.toList()),
                )
            }

        assertEquals(AuthErrorCode.NOT_FOUND_USER, exception.errorCode)
    }

    @Test
    fun `필수 약관에 모두 동의했으면 동의 완료로 조회된다`() {
        val port = FakeAgreementPort(initial = AgreementTermType.REQUIRED)

        assertTrue(service(agreements = port).hasAgreedRequiredTerms(userCode))
    }

    @Test
    fun `필수 약관이 하나라도 없으면 동의 미완료로 조회된다`() {
        val port = FakeAgreementPort(initial = setOf(AgreementTermType.TERMS_OF_SERVICE))

        assertFalse(service(agreements = port).hasAgreedRequiredTerms(userCode))
    }

    @Test
    fun `필수 약관은 이용약관, 개인정보, 만14세 세 가지다`() {
        assertEquals(
            setOf(
                AgreementTermType.TERMS_OF_SERVICE,
                AgreementTermType.PRIVACY_POLICY,
                AgreementTermType.AGE_OVER_14,
            ),
            AgreementTermType.REQUIRED,
        )
    }
}
