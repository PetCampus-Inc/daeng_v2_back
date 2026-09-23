package com.petcampus.knockdog.domain.bookmark.application.service

import com.petcampus.knockdog.domain.bookmark.application.BookmarkErrorCode
import com.petcampus.knockdog.domain.bookmark.application.port.output.BookmarkAddress
import com.petcampus.knockdog.domain.bookmark.application.port.output.BookmarkKindergarten
import com.petcampus.knockdog.domain.bookmark.application.port.output.BookmarkMemoSummary
import com.petcampus.knockdog.domain.bookmark.application.port.output.LoadBookmarkAddressesPort
import com.petcampus.knockdog.domain.bookmark.application.port.output.LoadBookmarkKindergartensPort
import com.petcampus.knockdog.domain.bookmark.application.port.output.LoadBookmarkMemoSummariesPort
import com.petcampus.knockdog.domain.bookmark.application.port.output.LoadBookmarkPort
import com.petcampus.knockdog.domain.bookmark.application.port.output.SaveBookmarkPort
import com.petcampus.knockdog.domain.bookmark.domain.Bookmark
import com.petcampus.knockdog.global.exception.BusinessException
import com.petcampus.knockdog.global.exception.CommonErrorCode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BookmarkServiceTest {
    private class RecordingSavePort : SaveBookmarkPort {
        val created = mutableListOf<Bookmark>()
        var deleted = false

        override fun createIfAbsent(bookmark: Bookmark) {
            created += bookmark
        }

        override fun delete(
            userCode: String,
            kindergartenId: String,
        ): Boolean = deleted
    }

    private class StubLoadPort(
        private val bookmarks: List<Bookmark> = emptyList(),
    ) : LoadBookmarkPort {
        override fun findAllByUserCode(userCode: String): List<Bookmark> = bookmarks
    }

    private class StubKindergartensPort(
        private val kindergartens: Map<String, BookmarkKindergarten>,
    ) : LoadBookmarkKindergartensPort {
        override fun findById(kindergartenId: String): BookmarkKindergarten? = kindergartens[kindergartenId]

        override fun findByIds(kindergartenIds: List<String>): List<BookmarkKindergarten> = kindergartenIds.mapNotNull { kindergartens[it] }
    }

    private class StubMemoPort(
        private val summaries: List<BookmarkMemoSummary> = emptyList(),
    ) : LoadBookmarkMemoSummariesPort {
        override fun findByUserCode(userCode: String): List<BookmarkMemoSummary> = summaries
    }

    private class StubAddressesPort(
        private val addresses: List<BookmarkAddress> = emptyList(),
    ) : LoadBookmarkAddressesPort {
        override fun findByUserCode(userCode: String): List<BookmarkAddress> = addresses
    }

    private fun kindergarten(
        id: String = "n-1",
        closed: Boolean = false,
    ) = BookmarkKindergarten(
        id = id,
        name = "유치원 $id",
        thumbnailS3Key = "thumbnail/$id.webp",
        categories = listOf("KINDERGARTEN"),
        location = "서울 강남구",
        price = 30000,
        reviewCount = 128,
        lat = 37.5,
        lng = 127.0,
        closed = closed,
    )

    private fun service(
        savePort: RecordingSavePort = RecordingSavePort(),
        bookmarks: List<Bookmark> = emptyList(),
        kindergartens: Map<String, BookmarkKindergarten> = emptyMap(),
        memoSummaries: List<BookmarkMemoSummary> = emptyList(),
        addresses: List<BookmarkAddress> = emptyList(),
    ) = BookmarkService(
        savePort,
        StubLoadPort(bookmarks),
        StubKindergartensPort(kindergartens),
        StubMemoPort(memoSummaries),
        StubAddressesPort(addresses),
    )

    @Test
    fun `활성 유치원을 저장한다`() {
        val savePort = RecordingSavePort()

        service(savePort = savePort, kindergartens = mapOf("n-1" to kindergarten())).create("A1B2C3D4", "n-1")

        assertEquals(listOf("n-1"), savePort.created.map { it.kindergartenId })
    }

    @Test
    fun `없는 유치원은 저장하지 않는다`() {
        val exception = assertFailsWith<BusinessException> { service().create("A1B2C3D4", "missing") }

        assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `폐업한 유치원은 저장하지 않는다`() {
        val exception =
            assertFailsWith<BusinessException> {
                service(kindergartens = mapOf("n-1" to kindergarten(closed = true))).create("A1B2C3D4", "n-1")
            }

        assertEquals(BookmarkErrorCode.CLOSED_SCHOOL, exception.errorCode)
    }

    @Test
    fun `목록은 저장 순서를 유지하고 카드 정보를 채운다`() {
        val result =
            service(
                bookmarks = listOf(Bookmark.create("A1B2C3D4", "n-2"), Bookmark.create("A1B2C3D4", "n-1")),
                kindergartens = mapOf("n-1" to kindergarten("n-1"), "n-2" to kindergarten("n-2")),
                memoSummaries = listOf(BookmarkMemoSummary("n-2", LocalDate.of(2026, 9, 16))),
                addresses = listOf(BookmarkAddress("HOME", 37.4, 127.1)),
            ).list("A1B2C3D4")

        val first = result.first()
        assertEquals(listOf("n-2", "n-1"), result.map { it.kindergarten.id })
        assertEquals(LocalDate.of(2026, 9, 16), first.memoDate)
        assertEquals("HOME", first.distances.single().referencePoint)
        assertTrue(
            first.distances
                .single()
                .distance
                .endsWith("km"),
        )
    }

    @Test
    fun `없어진 유치원은 목록에서 제외한다`() {
        val result =
            service(
                bookmarks = listOf(Bookmark.create("A1B2C3D4", "gone")),
            ).list("A1B2C3D4")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `없는 북마크를 해제하면 실패한다`() {
        val exception = assertFailsWith<BusinessException> { service().delete("A1B2C3D4", "n-1") }

        assertEquals(BookmarkErrorCode.NOT_FOUND, exception.errorCode)
    }
}
