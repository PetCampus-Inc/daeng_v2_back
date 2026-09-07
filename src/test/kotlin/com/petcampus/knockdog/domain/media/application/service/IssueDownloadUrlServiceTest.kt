package com.petcampus.knockdog.domain.media.application.service

import com.petcampus.knockdog.domain.media.application.port.input.IssueDownloadUrlCommand
import com.petcampus.knockdog.domain.media.application.port.output.ObjectStoragePort
import com.petcampus.knockdog.domain.media.application.port.output.PresignedUrl
import com.petcampus.knockdog.domain.media.domain.MediaContentType
import com.petcampus.knockdog.domain.media.domain.ObjectKey
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IssueDownloadUrlServiceTest {
    private class StubStoragePort : ObjectStoragePort {
        override fun createDownloadUrl(key: ObjectKey) = PresignedUrl("https://s3.example.com/get/${key.value}", expiresIn = 120)

        override fun createUploadUrl(
            key: ObjectKey,
            contentType: MediaContentType,
        ) = throw NotImplementedError()

        override fun exists(key: ObjectKey) = throw NotImplementedError()

        override fun copy(
            source: ObjectKey,
            destination: ObjectKey,
        ) = throw NotImplementedError()

        override fun delete(key: ObjectKey) = throw NotImplementedError()
    }

    @Test
    fun `임의의 key에 대해 다운로드 URL을 발급한다`() {
        val service = IssueDownloadUrlService(StubStoragePort())

        val result = service.issue(IssueDownloadUrlCommand(key = "kindergarten/1/thumbnail.webp"))

        assertEquals("https://s3.example.com/get/kindergarten/1/thumbnail.webp", result.url)
        assertEquals(120, result.expiresIn)
    }

    @Test
    fun `구조적으로 잘못된 key는 거부한다`() {
        val service = IssueDownloadUrlService(StubStoragePort())

        assertFailsWith<IllegalArgumentException> {
            service.issue(IssueDownloadUrlCommand(key = "../etc/passwd"))
        }
    }
}
