package com.petcampus.knockdog.domain.kindergarten.adapter.outbound.user

import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserPort
import com.petcampus.knockdog.domain.auth.domain.AddressType
import com.petcampus.knockdog.domain.auth.domain.User
import com.petcampus.knockdog.domain.auth.domain.UserAddress
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.auth.domain.UserId
import com.petcampus.knockdog.domain.kindergarten.domain.ComparisonReferencePoint
import com.petcampus.knockdog.domain.kindergarten.domain.ComparisonReferencePointType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ComparisonAddressAdapterTest {
    private class StubLoadUserPort(
        private val user: User?,
    ) : LoadUserPort {
        override fun findById(id: UserId) = user

        override fun findByCode(code: UserCode) = user
    }

    private fun user(vararg addresses: UserAddress) =
        User.reconstitute(
            id = UserId(1L),
            code = UserCode("A1B2C3D4"),
            nickname = null,
            profileImage = null,
            infoReceiveEmail = null,
            gender = null,
            phoneNumber = null,
            emergencyPhoneNumber = null,
            addresses = addresses.toList(),
            deletedAt = null,
        )

    private fun address(
        type: AddressType,
        lat: Double,
        lng: Double,
    ) = UserAddress.create(type, alias = null, address = "서울시", roadAddress = null, lat = lat, lng = lng)

    @Test
    fun `유저가 없으면 빈 목록이다`() {
        val adapter = ComparisonAddressAdapter(StubLoadUserPort(null))

        assertEquals(emptyList(), adapter.findByUserCode("A1B2C3D4"))
    }

    @Test
    fun `저장 주소를 타입 매핑해 기준점으로 변환한다`() {
        val adapter =
            ComparisonAddressAdapter(
                StubLoadUserPort(
                    user(
                        address(AddressType.HOME, 37.5, 127.0),
                        address(AddressType.OTHER, 37.6, 127.1),
                    ),
                ),
            )

        assertEquals(
            listOf(
                ComparisonReferencePoint(ComparisonReferencePointType.HOME, 37.5, 127.0),
                ComparisonReferencePoint(ComparisonReferencePointType.OTHER, 37.6, 127.1),
            ),
            adapter.findByUserCode("A1B2C3D4"),
        )
    }
}
