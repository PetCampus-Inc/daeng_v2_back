package com.petcampus.knockdog.domain.memo.application.service

import com.petcampus.knockdog.domain.memo.application.MemoErrorCode
import com.petcampus.knockdog.domain.memo.application.port.input.SaveMemoCommand
import com.petcampus.knockdog.domain.memo.application.port.output.LoadMemoPhotoPort
import com.petcampus.knockdog.domain.memo.application.port.output.LoadMemoPort
import com.petcampus.knockdog.domain.memo.application.port.output.MemoPhotoStoragePort
import com.petcampus.knockdog.domain.memo.application.port.output.MemoSummary
import com.petcampus.knockdog.domain.memo.application.port.output.SaveMemoPort
import com.petcampus.knockdog.domain.memo.domain.Memo
import com.petcampus.knockdog.domain.memo.domain.MemoId
import com.petcampus.knockdog.domain.memo.domain.MemoPhoto
import com.petcampus.knockdog.global.exception.BusinessException
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MemoServiceTest {
    private class FakeMemoPort :
        LoadMemoPort,
        SaveMemoPort {
        val store = mutableMapOf<Pair<String, String>, Memo>()
        private var seq = 100L

        override fun findByUserCodeAndTargetId(
            userCode: String,
            targetId: String,
        ) = store[userCode to targetId]

        override fun findSummariesByUserCode(userCode: String) =
            store.values
                .filter { it.userCode == userCode }
                .map { MemoSummary(it.targetId, it.content, LocalDate.of(2026, 9, 8)) }

        override fun save(memo: Memo): Memo {
            val persisted =
                if (memo.id == null) {
                    Memo.reconstitute(MemoId(seq++), memo.userCode, memo.targetId, memo.content)
                } else {
                    memo
                }
            store[persisted.userCode to persisted.targetId] = persisted
            return persisted
        }
    }

    private class FakePhotoLoadPort(
        private val photos: List<MemoPhoto>,
    ) : LoadMemoPhotoPort {
        override fun findAllByUserCodeAndTargetId(
            userCode: String,
            targetId: String,
        ) = photos.filter { it.userCode == userCode && it.targetId == targetId }

        override fun findById(photoId: Long) = photos.find { it.id == photoId }

        override fun countByUserCodeAndTargetId(
            userCode: String,
            targetId: String,
        ) = findAllByUserCodeAndTargetId(userCode, targetId).size
    }

    private val photoStorage =
        object : MemoPhotoStoragePort {
            override fun commitUploaded(
                userCode: String,
                uploadedKey: String,
            ) = throw UnsupportedOperationException()

            override fun viewUrlFor(objectKey: String) = "https://cdn/$objectKey"

            override fun delete(objectKey: String) = Unit
        }

    private fun service(
        memoPort: FakeMemoPort = FakeMemoPort(),
        photos: List<MemoPhoto> = emptyList(),
    ) = MemoService(memoPort, memoPort, FakePhotoLoadPort(photos), photoStorage)

    @Test
    fun `메모가 없으면 content null, photos 빈 리스트`() {
        val view = service().get("A1B2C3D4", "place-1")

        assertNull(view.content)
        assertEquals(emptyList(), view.photos)
    }

    @Test
    fun `save는 없으면 생성, 있으면 content 교체 — 사진은 안 건드린다`() {
        val port = FakeMemoPort()
        val photo = MemoPhoto.reconstitute(1L, "A1B2C3D4", "place-1", "memo/A1B2C3D4/x.webp", 0)
        val svc = service(port, listOf(photo))

        svc.save(SaveMemoCommand("A1B2C3D4", "place-1", "처음"))
        svc.save(SaveMemoCommand("A1B2C3D4", "place-1", "수정됨"))

        val view = svc.get("A1B2C3D4", "place-1")
        assertEquals("수정됨", view.content)
        assertEquals(1, port.store.size)
        assertEquals(listOf("memo/A1B2C3D4/x.webp"), view.photos.map { it.key })
        assertEquals("https://cdn/memo/A1B2C3D4/x.webp", view.photos[0].url)
    }

    @Test
    fun `content가 2000자를 넘으면 MEMO_CONTENT_TOO_LONG`() {
        val exception =
            assertFailsWith<BusinessException> {
                service().save(SaveMemoCommand("A1B2C3D4", "place-1", "가".repeat(2001)))
            }

        assertEquals(MemoErrorCode.CONTENT_TOO_LONG, exception.errorCode)
    }

    @Test
    fun `list는 유치원당 1건을 shopId·memoDate와 함께 반환`() {
        val svc = service()
        svc.save(SaveMemoCommand("A1B2C3D4", "place-1", "a"))

        val list = svc.list("A1B2C3D4")

        assertEquals(1, list.size)
        assertEquals("place-1", list[0].shopId)
        assertEquals(LocalDate.of(2026, 9, 8), list[0].memoDate)
    }
}
