package com.petcampus.knockdog.domain.media.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaContentTypeTest {
    @Test
    fun `허용된 MIME 타입이면 해당 항목을 돌려준다`() {
        assertEquals(MediaContentType.WEBP, MediaContentType.forMimeType("image/webp"))
    }

    @Test
    fun `허용되지 않은 MIME 타입이면 null을 돌려준다`() {
        assertNull(MediaContentType.forMimeType("application/pdf"))
    }

    @Test
    fun `항목마다 key 확장자를 가진다`() {
        assertEquals("jpg", MediaContentType.JPEG.extension)
    }

    @Test
    fun `HEIC와 HEIF를 허용한다`() {
        assertEquals(MediaContentType.HEIC, MediaContentType.forMimeType("image/heic"))
        assertEquals(MediaContentType.HEIF, MediaContentType.forMimeType("image/heif"))
    }
}
