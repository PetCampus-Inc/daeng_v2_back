package com.petcampus.knockdog.domain.media.application.service

import com.petcampus.knockdog.domain.media.application.port.input.CommitObjectCommand
import com.petcampus.knockdog.domain.media.application.port.output.ObjectStoragePort
import com.petcampus.knockdog.domain.media.application.port.output.PresignedUrl
import com.petcampus.knockdog.domain.media.domain.MediaContentType
import com.petcampus.knockdog.domain.media.domain.ObjectKey
import com.petcampus.knockdog.global.exception.BusinessException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CommitObjectServiceTest {
    private class FakeStoragePort(
        private val existingKeys: MutableSet<String> = mutableSetOf(),
    ) : ObjectStoragePort {
        val copies = mutableListOf<Pair<String, String>>()
        val deletions = mutableListOf<String>()

        fun seed(key: String) = existingKeys.add(key)

        override fun exists(key: ObjectKey) = existingKeys.contains(key.value)

        override fun copy(
            source: ObjectKey,
            destination: ObjectKey,
        ) {
            copies.add(source.value to destination.value)
            existingKeys.add(destination.value)
        }

        override fun delete(key: ObjectKey) {
            deletions.add(key.value)
            existingKeys.remove(key.value)
        }

        override fun createDownloadUrl(key: ObjectKey) = PresignedUrl("https://s3.example.com/get/${key.value}", expiresIn = 120)

        override fun createUploadUrl(
            key: ObjectKey,
            contentType: MediaContentType,
        ) = throw NotImplementedError()
    }

    @Test
    fun `임시 오브젝트를 영구 경로로 복사한 뒤 원본을 지운다`() {
        val port = FakeStoragePort()
        port.seed("tmp/A1B2C3D4/photo.webp")
        val service = CommitObjectService(port)

        val result =
            service.commit(
                CommitObjectCommand(userCode = "A1B2C3D4", key = "tmp/A1B2C3D4/photo.webp", targetPath = "memo/42"),
            )

        assertEquals("memo/42/photo.webp", result.key)
        assertEquals("https://s3.example.com/get/memo/42/photo.webp", result.url)
        assertEquals(listOf("tmp/A1B2C3D4/photo.webp" to "memo/42/photo.webp"), port.copies)
        assertEquals(listOf("tmp/A1B2C3D4/photo.webp"), port.deletions)
    }

    @Test
    fun `targetPath 뒤 슬래시는 무시한다`() {
        val port = FakeStoragePort()
        port.seed("tmp/A1B2C3D4/photo.webp")
        val service = CommitObjectService(port)

        val result =
            service.commit(
                CommitObjectCommand(userCode = "A1B2C3D4", key = "tmp/A1B2C3D4/photo.webp", targetPath = "memo/42/"),
            )

        assertEquals("memo/42/photo.webp", result.key)
    }

    @Test
    fun `호출자의 임시 네임스페이스가 아닌 key는 거부하고 저장소를 건드리지 않는다`() {
        val port = FakeStoragePort()
        port.seed("tmp/ZZZZZZZZ/photo.webp")
        val service = CommitObjectService(port)

        val exception =
            assertFailsWith<BusinessException> {
                service.commit(
                    CommitObjectCommand(userCode = "A1B2C3D4", key = "tmp/ZZZZZZZZ/photo.webp", targetPath = "memo/42"),
                )
            }

        assertEquals("MEDIA_FORBIDDEN_KEY", exception.errorCode.code)
        assertTrue(port.copies.isEmpty())
        assertTrue(port.deletions.isEmpty())
    }

    @Test
    fun `targetPath가 임시 영역이면 거부한다`() {
        val port = FakeStoragePort()
        port.seed("tmp/A1B2C3D4/photo.webp")
        val service = CommitObjectService(port)

        val exception =
            assertFailsWith<BusinessException> {
                service.commit(
                    CommitObjectCommand(userCode = "A1B2C3D4", key = "tmp/A1B2C3D4/photo.webp", targetPath = "tmp/A1B2C3D4"),
                )
            }

        assertEquals("MEDIA_INVALID_TARGET_PATH", exception.errorCode.code)
    }

    @Test
    fun `원본이 존재하지 않으면 OBJECT_NOT_FOUND를 던진다`() {
        val port = FakeStoragePort()
        val service = CommitObjectService(port)

        val exception =
            assertFailsWith<BusinessException> {
                service.commit(
                    CommitObjectCommand(userCode = "A1B2C3D4", key = "tmp/A1B2C3D4/photo.webp", targetPath = "memo/42"),
                )
            }

        assertEquals("MEDIA_OBJECT_NOT_FOUND", exception.errorCode.code)
        assertTrue(port.copies.isEmpty())
    }
}
