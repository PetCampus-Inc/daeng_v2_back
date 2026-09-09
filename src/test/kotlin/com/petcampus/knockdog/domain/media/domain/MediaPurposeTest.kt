package com.petcampus.knockdog.domain.media.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaPurposeTest {
    @Test
    fun `이름으로 purpose를 찾는다`() {
        assertEquals(MediaPurpose.PROFILE_IMAGE, MediaPurpose.from("PROFILE_IMAGE"))
    }

    @Test
    fun `지원하지 않는 값이면 null이다`() {
        assertNull(MediaPurpose.from("MEMO_ATTACHMENT"))
        assertNull(MediaPurpose.from("profile_image"))
    }

    @Test
    fun `PROFILE_IMAGE의 영구 key는 호출자 폴더 아래다`() {
        val key = MediaPurpose.PROFILE_IMAGE.permanentKey("A1B2C3D4", "9f3c.webp")

        assertEquals("user/A1B2C3D4/9f3c.webp", key.value)
    }
}
