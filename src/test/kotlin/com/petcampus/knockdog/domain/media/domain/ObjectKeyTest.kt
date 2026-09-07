package com.petcampus.knockdog.domain.media.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ObjectKeyTest {
    @Test
    fun `임시 key는 호출자 네임스페이스 아래에 uuid와 확장자로 생성된다`() {
        val key = ObjectKey.temporary("A1B2C3D4", MediaContentType.WEBP)

        assertTrue(key.value.startsWith("tmp/A1B2C3D4/"))
        assertTrue(key.value.endsWith(".webp"))
    }

    @Test
    fun `임시 key는 매번 다른 값을 만든다`() {
        val first = ObjectKey.temporary("A1B2C3D4", MediaContentType.PNG)
        val second = ObjectKey.temporary("A1B2C3D4", MediaContentType.PNG)

        assertFalse(first == second)
    }

    @Test
    fun `자신의 임시 네임스페이스에 속하는지 판별한다`() {
        val key = ObjectKey.temporary("A1B2C3D4", MediaContentType.JPEG)

        assertTrue(key.isInTemporaryAreaOf("A1B2C3D4"))
        assertFalse(key.isInTemporaryAreaOf("ZZZZZZZZ"))
    }

    @Test
    fun `다른 사용자의 임시 경로를 자신의 것으로 오인하지 않는다`() {
        val key = ObjectKey("tmp/A1B2C3D4EXTRA/x.png")

        assertFalse(key.isInTemporaryAreaOf("A1B2C3D4"))
    }

    @Test
    fun `filename은 마지막 경로 구획이다`() {
        assertEquals("photo.webp", ObjectKey("tmp/A1B2C3D4/photo.webp").filename)
    }

    @Test
    fun `상위 경로 탈출 구획을 가진 key는 거부한다`() {
        assertFailsWith<IllegalArgumentException> { ObjectKey("tmp/A1B2C3D4/../secret.png") }
    }

    @Test
    fun `절대 경로 key는 거부한다`() {
        assertFailsWith<IllegalArgumentException> { ObjectKey("/tmp/A1B2C3D4/x.png") }
    }

    @Test
    fun `빈 key는 거부한다`() {
        assertFailsWith<IllegalArgumentException> { ObjectKey(" ") }
    }
}
