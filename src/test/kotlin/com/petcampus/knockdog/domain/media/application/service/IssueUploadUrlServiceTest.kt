package com.petcampus.knockdog.domain.media.application.service

import com.petcampus.knockdog.domain.media.application.port.input.IssueUploadUrlCommand
import com.petcampus.knockdog.domain.media.application.port.output.ObjectStoragePort
import com.petcampus.knockdog.domain.media.application.port.output.PresignedUrl
import com.petcampus.knockdog.domain.media.domain.MediaContentType
import com.petcampus.knockdog.domain.media.domain.ObjectKey
import com.petcampus.knockdog.global.exception.BusinessException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class IssueUploadUrlServiceTest {
    private class RecordingStoragePort : ObjectStoragePort {
        var lastUploadKey: ObjectKey? = null

        override fun createUploadUrl(
            key: ObjectKey,
            contentType: MediaContentType,
        ): PresignedUrl {
            lastUploadKey = key
            return PresignedUrl("https://s3.example.com/put/${key.value}", expiresIn = 600)
        }

        override fun createDownloadUrl(key: ObjectKey) = throw NotImplementedError()

        override fun exists(key: ObjectKey) = throw NotImplementedError()

        override fun copy(
            source: ObjectKey,
            destination: ObjectKey,
        ) = throw NotImplementedError()

        override fun delete(key: ObjectKey) = throw NotImplementedError()
    }

    @Test
    fun `업로드 key는 호출자의 임시 네임스페이스와 purpose로 강제된다`() {
        val port = RecordingStoragePort()
        val service = IssueUploadUrlService(port)

        val result = service.issue(IssueUploadUrlCommand(userCode = "A1B2C3D4", purpose = "PROFILE_IMAGE", contentType = "image/webp"))

        assertTrue(port.lastUploadKey!!.isInTemporaryAreaOf("A1B2C3D4"))
        assertTrue(result.key.startsWith("tmp/A1B2C3D4/PROFILE_IMAGE/"))
        assertEquals("https://s3.example.com/put/${result.key}", result.url)
        assertEquals(600, result.expiresIn)
    }

    @Test
    fun `허용되지 않은 content-type이면 거부한다`() {
        val service = IssueUploadUrlService(RecordingStoragePort())

        val exception =
            assertFailsWith<BusinessException> {
                service.issue(IssueUploadUrlCommand(userCode = "A1B2C3D4", purpose = "PROFILE_IMAGE", contentType = "application/pdf"))
            }

        assertEquals("MEDIA_UNSUPPORTED_CONTENT_TYPE", exception.errorCode.code)
    }

    @Test
    fun `지원하지 않는 purpose면 거부한다`() {
        val service = IssueUploadUrlService(RecordingStoragePort())

        val exception =
            assertFailsWith<BusinessException> {
                service.issue(IssueUploadUrlCommand(userCode = "A1B2C3D4", purpose = "MEMO_ATTACHMENT", contentType = "image/webp"))
            }

        assertEquals("MEDIA_UNSUPPORTED_PURPOSE", exception.errorCode.code)
    }
}
