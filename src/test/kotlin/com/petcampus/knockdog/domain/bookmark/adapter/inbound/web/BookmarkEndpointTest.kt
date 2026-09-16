package com.petcampus.knockdog.domain.bookmark.adapter.inbound.web

import com.petcampus.knockdog.domain.auth.application.port.output.TokenPort
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.bookmark.adapter.outbound.persistence.BookmarkJpaRepository
import com.petcampus.knockdog.domain.bookmark.application.port.output.BookmarkAddress
import com.petcampus.knockdog.domain.bookmark.application.port.output.BookmarkKindergarten
import com.petcampus.knockdog.domain.bookmark.application.port.output.BookmarkMemoSummary
import com.petcampus.knockdog.domain.bookmark.application.port.output.LoadBookmarkAddressesPort
import com.petcampus.knockdog.domain.bookmark.application.port.output.LoadBookmarkKindergartensPort
import com.petcampus.knockdog.domain.bookmark.application.port.output.LoadBookmarkMemoSummariesPort
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import kotlin.test.assertEquals

@SpringBootTest
@AutoConfigureMockMvc
@Import(BookmarkEndpointTest.FakeBookmarkDependencies::class)
class BookmarkEndpointTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var tokenPort: TokenPort

    @Autowired
    private lateinit var bookmarkJpaRepository: BookmarkJpaRepository

    private val userCode = "A1B2C3D4"

    private fun bearer() = "Bearer " + tokenPort.issueAccessToken(UserCode(userCode))

    @BeforeEach
    fun clean() {
        bookmarkJpaRepository.deleteAll()
    }

    @Test
    fun `인증 없이 북마크를 조회하면 401이다`() {
        mockMvc.perform(get("/api/v1/users/me/bookmarks")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `북마크를 저장하고 카드 목록을 조회한다`() {
        mockMvc
            .perform(put("/api/v1/kindergartens/n-1/bookmark").header("Authorization", bearer()))
            .andExpect(status().isOk)

        mockMvc
            .perform(get("/api/v1/users/me/bookmarks").header("Authorization", bearer()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].kindergarten.id").value("n-1"))
            .andExpect(jsonPath("$.data[0].memoDate").value("2026-09-16"))
            .andExpect(jsonPath("$.data[0].distances[0].referencePoint").value("HOME"))
    }

    @Test
    fun `같은 유치원을 다시 저장해도 한 행만 남는다`() {
        mockMvc.perform(put("/api/v1/kindergartens/n-1/bookmark").header("Authorization", bearer()))
        mockMvc.perform(put("/api/v1/kindergartens/n-1/bookmark").header("Authorization", bearer()))

        assertEquals(1, bookmarkJpaRepository.count())
    }

    @Test
    fun `북마크를 해제한다`() {
        mockMvc.perform(put("/api/v1/kindergartens/n-1/bookmark").header("Authorization", bearer()))

        mockMvc
            .perform(delete("/api/v1/kindergartens/n-1/bookmark").header("Authorization", bearer()))
            .andExpect(status().isOk)

        mockMvc
            .perform(get("/api/v1/users/me/bookmarks").header("Authorization", bearer()))
            .andExpect(jsonPath("$.data.length()").value(0))
    }

    @Test
    fun `없는 북마크를 해제하면 404다`() {
        mockMvc
            .perform(delete("/api/v1/kindergartens/n-1/bookmark").header("Authorization", bearer()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("BOOKMARK_NOT_FOUND"))
    }

    @Test
    fun `폐업한 유치원을 저장하면 400이다`() {
        mockMvc
            .perform(put("/api/v1/kindergartens/closed/bookmark").header("Authorization", bearer()))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BOOKMARK_CLOSED_SCHOOL"))
    }

    @TestConfiguration
    class FakeBookmarkDependencies {
        @Bean
        @Primary
        fun fakeKindergartensPort(): LoadBookmarkKindergartensPort =
            object : LoadBookmarkKindergartensPort {
                private val kindergartens =
                    mapOf(
                        "n-1" to kindergarten("n-1"),
                        "closed" to kindergarten("closed", closed = true),
                    )

                override fun findById(kindergartenId: String): BookmarkKindergarten? = kindergartens[kindergartenId]

                override fun findByIds(kindergartenIds: List<String>): List<BookmarkKindergarten> =
                    kindergartenIds.mapNotNull { kindergartens[it] }
            }

        @Bean
        @Primary
        fun fakeMemoSummariesPort(): LoadBookmarkMemoSummariesPort =
            object : LoadBookmarkMemoSummariesPort {
                override fun findByUserCode(userCode: String): List<BookmarkMemoSummary> =
                    listOf(BookmarkMemoSummary("n-1", LocalDate.of(2026, 9, 16)))
            }

        @Bean
        @Primary
        fun fakeAddressesPort(): LoadBookmarkAddressesPort =
            object : LoadBookmarkAddressesPort {
                override fun findByUserCode(userCode: String): List<BookmarkAddress> = listOf(BookmarkAddress("HOME", 37.4, 127.1))
            }

        private fun kindergarten(
            id: String,
            closed: Boolean = false,
        ) = BookmarkKindergarten(
            id = id,
            name = "유치원 $id",
            thumbnailS3Key = null,
            categories = listOf("KINDERGARTEN"),
            location = "서울 강남구",
            price = 30000,
            reviewCount = 128,
            lat = 37.5,
            lng = 127.0,
            closed = closed,
        )
    }
}
