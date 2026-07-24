package com.petcampus.knockdog.domain.owner.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 순수 도메인 단위 테스트 — Spring 컨텍스트도 DB도 필요 없다.
 */
class OwnerTest {
    @Test
    fun `회원을 등록하면 ACTIVE 상태가 된다`() {
        val owner = Owner.register(Email("test@knockdog.com"))

        assertEquals(OwnerStatus.ACTIVE, owner.status)
    }

    @Test
    fun `탈퇴하면 WITHDRAWN 상태가 된다`() {
        val owner = Owner.register(Email("test@knockdog.com"))

        owner.withdraw()

        assertEquals(OwnerStatus.WITHDRAWN, owner.status)
    }

    @Test
    fun `이미 탈퇴한 회원은 다시 탈퇴할 수 없다`() {
        val owner = Owner.register(Email("test@knockdog.com"))
        owner.withdraw()

        assertFailsWith<IllegalArgumentException> {
            owner.withdraw()
        }
    }

    @Test
    fun `잘못된 이메일 형식은 거부된다`() {
        assertFailsWith<IllegalArgumentException> {
            Email("not-an-email")
        }
    }
}
