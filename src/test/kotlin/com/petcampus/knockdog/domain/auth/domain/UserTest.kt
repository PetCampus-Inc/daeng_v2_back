package com.petcampus.knockdog.domain.auth.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserTest {
    private fun homeAddress() = UserAddress.create(AddressType.HOME, null, "서울시 강남구", null, 37.5, 127.0)

    @Test
    fun `HOME 타입 주소가 없으면 회원가입에 실패한다`() {
        val otherAddress = UserAddress.create(AddressType.OTHER, null, "서울시 서초구", null, 37.4, 127.1)

        assertFailsWith<IllegalArgumentException> {
            User.create("홍길동", null, null, listOf(otherAddress))
        }
    }

    /** 레거시가 KD3-372에서 WORK를 OTHER로 통합했다. 신규 서버가 되살리지 않는지 고정한다. */
    @Test
    fun `주소 타입은 HOME과 OTHER 두 가지뿐이다`() {
        assertEquals(listOf(AddressType.HOME, AddressType.OTHER), AddressType.entries.toList())
    }

    /**
     * v0 계약: 프론트가 `address.id`로 주소 수정·삭제를, `addressDetail`을 화면 표시에 쓴다.
     * 응답에서 빠지면 주소 수정·삭제가 깨진다.
     */
    @Test
    fun `영속성에서 복원한 주소는 id와 addressDetail을 갖는다`() {
        val address =
            UserAddress.reconstitute(
                id = 7L,
                type = AddressType.HOME,
                alias = null,
                address = "서울시 강남구",
                roadAddress = null,
                addressDetail = "101동 202호",
                lat = 37.5,
                lng = 127.0,
            )

        assertEquals(7L, address.id)
        assertEquals("101동 202호", address.addressDetail)
    }

    /** 신규 가입 시점에는 아직 PK가 없다. */
    @Test
    fun `신규 생성한 주소는 id가 없다`() {
        assertNull(homeAddress().id)
    }

    /** 레거시가 KD3-372에서 user.nickname을 NULL 허용으로 바꿨다(RegisterRequest에도 @NotBlank 없음). */
    @Test
    fun `닉네임 없이도 회원가입할 수 있다`() {
        val user = User.create(null, null, null, listOf(homeAddress()))

        assertNull(user.nickname)
        assertEquals(8, user.code.value.length)
    }

    @Test
    fun `HOME 타입 주소가 있으면 회원가입에 성공하고 8자 UserCode가 생성된다`() {
        val user = User.create("홍길동", null, null, listOf(homeAddress()))

        assertEquals(8, user.code.value.length)
        assertFalse(user.isWithdrawn)
    }

    @Test
    fun `탈퇴하면 deletedAt이 채워지고 isWithdrawn이 true가 된다`() {
        val user = User.create("홍길동", null, null, listOf(homeAddress()))

        user.withdraw()

        assertTrue(user.isWithdrawn)
    }

    @Test
    fun `이미 탈퇴한 회원은 다시 탈퇴할 수 없다`() {
        val user = User.create("홍길동", null, null, listOf(homeAddress()))
        user.withdraw()

        assertFailsWith<IllegalStateException> {
            user.withdraw()
        }
    }
}
