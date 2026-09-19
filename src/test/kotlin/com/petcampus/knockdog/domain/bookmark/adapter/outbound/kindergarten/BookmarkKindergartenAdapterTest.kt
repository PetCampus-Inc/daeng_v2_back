package com.petcampus.knockdog.domain.bookmark.adapter.outbound.kindergarten

import com.petcampus.knockdog.domain.kindergarten.application.port.output.KindergartenCardSummary
import com.petcampus.knockdog.domain.kindergarten.application.port.output.LoadKindergartenPort
import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class BookmarkKindergartenAdapterTest {
    private class StubLoadKindergartenPort(
        private val missingIds: Set<String> = emptySet(),
    ) : LoadKindergartenPort {
        override fun findByNaverPlaceId(naverPlaceId: String): Kindergarten? = null

        override fun findByNaverPlaceIds(naverPlaceIds: List<String>): List<Kindergarten> = emptyList()

        override fun findCardSummariesByNaverPlaceIds(naverPlaceIds: List<String>): List<KindergartenCardSummary> =
            naverPlaceIds.filterNot { it in missingIds }.map { id ->
                KindergartenCardSummary(
                    id = id,
                    name = "유치원 $id",
                    thumbnailS3Key = "thumbnail/$id.webp",
                    categories = listOf("KINDERGARTEN"),
                    address = "서울 강남구",
                    lowestPrice = 30000,
                    blogReviewCount = 128,
                    lat = 37.5,
                    lng = 127.0,
                    closed = false,
                )
            }
    }

    @Test
    fun `카드 요약을 북마크 목록 정보로 변환한다`() {
        val adapter = BookmarkKindergartenAdapter(StubLoadKindergartenPort())

        val result = adapter.findByIds(listOf("n-2", "n-1"))

        assertEquals(listOf("n-2", "n-1"), result.map { it.id })
        assertEquals(30000, result.first().price)
        assertEquals(128, result.first().reviewCount)
        assertFalse(result.first().closed)
    }

    @Test
    fun `단건 조회는 카드 요약을 변환하고 없으면 null이다`() {
        val adapter = BookmarkKindergartenAdapter(StubLoadKindergartenPort(missingIds = setOf("gone")))

        assertEquals("n-1", adapter.findById("n-1")?.id)
        assertNull(adapter.findById("gone"))
    }

    @Test
    fun `빈 목록 조회는 유치원 조회 없이 빈 결과를 돌려준다`() {
        val adapter = BookmarkKindergartenAdapter(StubLoadKindergartenPort())

        assertEquals(emptyList(), adapter.findByIds(emptyList()))
    }
}
