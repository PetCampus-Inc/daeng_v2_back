package com.petcampus.knockdog.domain.memo.application.service

import com.petcampus.knockdog.domain.memo.application.MemoErrorCode
import com.petcampus.knockdog.domain.memo.application.port.input.AddMemoPhotoCommand
import com.petcampus.knockdog.domain.memo.application.port.input.DeleteMemoPhotoCommand
import com.petcampus.knockdog.domain.memo.application.port.output.CommittedPhoto
import com.petcampus.knockdog.domain.memo.application.port.output.LoadMemoPhotoPort
import com.petcampus.knockdog.domain.memo.application.port.output.MemoPhotoStoragePort
import com.petcampus.knockdog.domain.memo.application.port.output.SaveMemoPhotoPort
import com.petcampus.knockdog.domain.memo.domain.MemoPhoto
import com.petcampus.knockdog.global.exception.BusinessException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MemoPhotoServiceTest {
    private class FakePhotoPort :
        LoadMemoPhotoPort,
        SaveMemoPhotoPort {
        val store = mutableListOf<MemoPhoto>()
        private var seq = 1L

        override fun findAllByUserCodeAndTargetId(
            userCode: String,
            targetId: String,
        ) = store.filter { it.userCode == userCode && it.targetId == targetId }.sortedBy { it.sortOrder }

        override fun findById(photoId: Long) = store.find { it.id == photoId }

        override fun countByUserCodeAndTargetId(
            userCode: String,
            targetId: String,
        ) = findAllByUserCodeAndTargetId(userCode, targetId).size

        override fun save(photo: MemoPhoto): MemoPhoto {
            val persisted =
                MemoPhoto.reconstitute(seq++, photo.userCode, photo.targetId, photo.objectKey, photo.sortOrder)
            store.add(persisted)
            return persisted
        }

        override fun deleteById(photoId: Long) {
            store.removeIf { it.id == photoId }
        }
    }

    private val deleted = mutableListOf<String>()

    private val storage =
        object : MemoPhotoStoragePort {
            override fun commitUploaded(
                userCode: String,
                uploadedKey: String,
            ) = CommittedPhoto("memo/$userCode/" + uploadedKey.substringAfterLast('/'))

            override fun viewUrlFor(objectKey: String) = "https://cdn/$objectKey"

            override fun delete(objectKey: String) {
                deleted.add(objectKey)
            }
        }

    private fun service(port: FakePhotoPort = FakePhotoPort()) = MemoPhotoService(port, port, storage)

    @Test
    fun `add는 tmp key를 commit해 저장하고 다음 sortOrder를 부여한다`() {
        val port = FakePhotoPort()
        val svc = service(port)

        svc.add(AddMemoPhotoCommand("A1B2C3D4", "place-1", "tmp/A1B2C3D4/MEMO_ATTACHMENT/a.webp"))
        val second = svc.add(AddMemoPhotoCommand("A1B2C3D4", "place-1", "tmp/A1B2C3D4/MEMO_ATTACHMENT/b.webp"))

        assertEquals(2, port.countByUserCodeAndTargetId("A1B2C3D4", "place-1"))
        assertEquals("memo/A1B2C3D4/b.webp", second.key)
        assertEquals(1, port.store.last().sortOrder)
    }

    @Test
    fun `tmp가 아닌 key는 MEMO_INVALID_PHOTO_KEY`() {
        val exception =
            assertFailsWith<BusinessException> {
                service().add(AddMemoPhotoCommand("A1B2C3D4", "place-1", "memo/A1B2C3D4/a.webp"))
            }
        assertEquals(MemoErrorCode.INVALID_PHOTO_KEY, exception.errorCode)
    }

    @Test
    fun `6장째 추가는 MEMO_TOO_MANY_PHOTOS`() {
        val port = FakePhotoPort()
        val svc = service(port)
        repeat(5) { svc.add(AddMemoPhotoCommand("A1B2C3D4", "place-1", "tmp/A1B2C3D4/MEMO_ATTACHMENT/$it.webp")) }

        val exception =
            assertFailsWith<BusinessException> {
                svc.add(AddMemoPhotoCommand("A1B2C3D4", "place-1", "tmp/A1B2C3D4/MEMO_ATTACHMENT/x.webp"))
            }
        assertEquals(MemoErrorCode.TOO_MANY_PHOTOS, exception.errorCode)
    }

    @Test
    fun `delete는 소유자 확인 후 row와 S3 object를 지운다`() {
        val port = FakePhotoPort()
        val svc = service(port)
        val added = svc.add(AddMemoPhotoCommand("A1B2C3D4", "place-1", "tmp/A1B2C3D4/MEMO_ATTACHMENT/a.webp"))

        svc.delete(DeleteMemoPhotoCommand("A1B2C3D4", "place-1", added.id))

        assertEquals(0, port.store.size)
        assertEquals(listOf(added.key), deleted)
    }

    @Test
    fun `남의 사진 삭제는 MEMO_PHOTO_NOT_FOUND`() {
        val port = FakePhotoPort()
        val svc = service(port)
        val added = svc.add(AddMemoPhotoCommand("A1B2C3D4", "place-1", "tmp/A1B2C3D4/MEMO_ATTACHMENT/a.webp"))

        val exception =
            assertFailsWith<BusinessException> {
                svc.delete(DeleteMemoPhotoCommand("OTHER999", "place-1", added.id))
            }
        assertEquals(MemoErrorCode.PHOTO_NOT_FOUND, exception.errorCode)
    }
}
