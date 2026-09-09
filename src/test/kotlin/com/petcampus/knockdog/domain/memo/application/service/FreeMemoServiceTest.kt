package com.petcampus.knockdog.domain.memo.application.service

import com.petcampus.knockdog.domain.memo.application.MemoErrorCode
import com.petcampus.knockdog.domain.memo.application.port.input.SaveFreeMemoCommand
import com.petcampus.knockdog.domain.memo.application.port.output.CommittedPhoto
import com.petcampus.knockdog.domain.memo.application.port.output.LoadFreeMemoPort
import com.petcampus.knockdog.domain.memo.application.port.output.MemoPhotoStoragePort
import com.petcampus.knockdog.domain.memo.application.port.output.MemoSummary
import com.petcampus.knockdog.domain.memo.application.port.output.SaveFreeMemoPort
import com.petcampus.knockdog.domain.memo.domain.FreeMemo
import com.petcampus.knockdog.domain.memo.domain.MemoId
import com.petcampus.knockdog.global.exception.BusinessException
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class FreeMemoServiceTest {
    private class FakeMemoPort :
        LoadFreeMemoPort,
        SaveFreeMemoPort {
        val store = mutableMapOf<Pair<String, String>, FreeMemo>()
        private var seq = 100L

        override fun findByUserCodeAndTargetId(
            userCode: String,
            targetId: String,
        ) = store[userCode to targetId]

        override fun findSummariesByUserCode(userCode: String) =
            store.values
                .filter { it.userCode == userCode }
                .map { MemoSummary(it.targetId, it.content, LocalDate.of(2026, 9, 8)) }

        override fun save(memo: FreeMemo): FreeMemo {
            val persisted =
                if (memo.id == null) {
                    FreeMemo.reconstitute(MemoId(seq++), memo.userCode, memo.targetId, memo.content, memo.photos)
                } else {
                    memo
                }
            store[persisted.userCode to persisted.targetId] = persisted
            return persisted
        }
    }

    private class FakePhotoStorage : MemoPhotoStoragePort {
        override fun commitUploaded(
            userCode: String,
            uploadedKey: String,
        ) = CommittedPhoto("memo/$userCode/" + uploadedKey.substringAfterLast('/'))

        override fun viewUrlFor(objectKey: String) = "https://cdn/$objectKey"
    }

    private fun service(
        port: FakeMemoPort = FakeMemoPort(),
        storage: MemoPhotoStoragePort = FakePhotoStorage(),
    ) = FreeMemoService(port, port, storage)

    @Test
    fun `메모가 없으면 content null, photos 빈 리스트`() {
        val view = service().get("A1B2C3D4", "place-1")

        assertNull(view.content)
        assertEquals(emptyList(), view.photos)
    }

    @Test
    fun `save는 없으면 생성, 있으면 content 교체`() {
        val port = FakeMemoPort()
        val svc = service(port)

        svc.save(SaveFreeMemoCommand("A1B2C3D4", "place-1", "처음", emptyList()))
        svc.save(SaveFreeMemoCommand("A1B2C3D4", "place-1", "수정됨", emptyList()))

        assertEquals("수정됨", svc.get("A1B2C3D4", "place-1").content)
        assertEquals(1, port.store.size)
    }

    @Test
    fun `content가 2000자를 넘으면 MEMO_CONTENT_TOO_LONG`() {
        val exception =
            assertFailsWith<BusinessException> {
                service().save(SaveFreeMemoCommand("A1B2C3D4", "place-1", "가".repeat(2001), emptyList()))
            }

        assertEquals(MemoErrorCode.CONTENT_TOO_LONG, exception.errorCode)
    }

    @Test
    fun `list는 유치원당 1건을 shopId·memoDate와 함께 반환`() {
        val svc = service()
        svc.save(SaveFreeMemoCommand("A1B2C3D4", "place-1", "a", emptyList()))

        val list = svc.list("A1B2C3D4")

        assertEquals(1, list.size)
        assertEquals("place-1", list[0].shopId)
        assertEquals(LocalDate.of(2026, 9, 8), list[0].memoDate)
    }

    @Test
    fun `save는 tmp key를 commit해 영구 key로 저장하고 GET에서 url을 채운다`() {
        val svc = service()
        svc.save(SaveFreeMemoCommand("A1B2C3D4", "place-1", "m", listOf("tmp/A1B2C3D4/MEMO_ATTACHMENT/u.webp")))

        val view = svc.get("A1B2C3D4", "place-1")

        assertEquals(1, view.photos.size)
        assertEquals("memo/A1B2C3D4/u.webp", view.photos[0].key)
        assertEquals("https://cdn/memo/A1B2C3D4/u.webp", view.photos[0].url)
    }

    @Test
    fun `이미 영구인 내 memo 소유 key는 그대로 유지된다`() {
        val svc = service()
        svc.save(SaveFreeMemoCommand("A1B2C3D4", "p", null, listOf("memo/A1B2C3D4/kept.webp")))

        assertEquals("memo/A1B2C3D4/kept.webp", svc.get("A1B2C3D4", "p").photos[0].key)
    }

    @Test
    fun `남의 네임스페이스 key는 MEMO_INVALID_PHOTO_KEY`() {
        val exception =
            assertFailsWith<BusinessException> {
                service().save(SaveFreeMemoCommand("A1B2C3D4", "p", null, listOf("memo/OTHER999/x.webp")))
            }

        assertEquals(MemoErrorCode.INVALID_PHOTO_KEY, exception.errorCode)
    }

    @Test
    fun `photoKeys에 중복 key가 있으면 MEMO_INVALID_PHOTO_KEY`() {
        val exception =
            assertFailsWith<BusinessException> {
                service().save(
                    SaveFreeMemoCommand(
                        "A1B2C3D4",
                        "p",
                        null,
                        listOf("memo/A1B2C3D4/x.webp", "memo/A1B2C3D4/x.webp"),
                    ),
                )
            }

        assertEquals(MemoErrorCode.INVALID_PHOTO_KEY, exception.errorCode)
    }

    @Test
    fun `사진 6장이면 MEMO_TOO_MANY_PHOTOS`() {
        val keys = (1..6).map { "tmp/A1B2C3D4/MEMO_ATTACHMENT/$it.webp" }

        val exception =
            assertFailsWith<BusinessException> {
                service().save(SaveFreeMemoCommand("A1B2C3D4", "p", null, keys))
            }

        assertEquals(MemoErrorCode.TOO_MANY_PHOTOS, exception.errorCode)
    }
}
