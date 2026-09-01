package com.petcampus.knockdog.domain.auth.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SocialUserTest {
    @Test
    fun `생성 직후에는 status로 넘긴 값이 그대로 유지된다`() {
        val socialUser = SocialUser.create(Provider.GOOGLE, "g-1", "a@b.com", "홍길동", null, SocialUserStatus.UNLINKED)

        assertEquals(SocialUserStatus.UNLINKED, socialUser.status)
        assertFalse(socialUser.isLinked)
        assertNull(socialUser.userId)
    }

    @Test
    fun `link하면 LINKED 상태가 되고 userId가 채워진다`() {
        val socialUser = SocialUser.create(Provider.GOOGLE, "g-1", "a@b.com", "홍길동", null, SocialUserStatus.UNLINKED)

        socialUser.link(UserId(1L))

        assertTrue(socialUser.isLinked)
        assertEquals(UserId(1L), socialUser.userId)
    }

    @Test
    fun `unlink하면 UNLINKED 상태가 되고 userId가 비워진다`() {
        val socialUser = SocialUser.create(Provider.GOOGLE, "g-1", "a@b.com", "홍길동", null, SocialUserStatus.UNLINKED)
        socialUser.link(UserId(1L))

        socialUser.unlink()

        assertEquals(SocialUserStatus.UNLINKED, socialUser.status)
        assertFalse(socialUser.isLinked)
        assertNull(socialUser.userId)
    }
}
