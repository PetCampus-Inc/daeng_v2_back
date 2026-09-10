package com.petcampus.knockdog.domain.memo.adapter.inbound.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.petcampus.knockdog.domain.auth.application.port.output.TokenPort
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.media.adapter.inbound.web.MediaEndpointsTest
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(MediaEndpointsTest.FakeStorageConfig::class)
class MemoPhotoEndpointsTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var tokenPort: TokenPort

    private fun bearer(userCode: String = "A1B2C3D4") = "Bearer " + tokenPort.issueAccessToken(UserCode(userCode))

    private fun uploadTmpKey(): String {
        val response =
            mockMvc
                .perform(
                    post("/api/v1/media/upload-urls")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"purpose":"MEMO_ATTACHMENT","contentType":"image/webp"}"""),
                ).andReturn()
                .response
                .contentAsString
        return ObjectMapper().readTree(response).at("/data/key").asText()
    }

    @Test
    fun `POST로 사진을 추가하면 GET 메모에 photos가 채워진다`() {
        mockMvc
            .perform(
                post("/api/v1/memos/place-1/photos")
                    .header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"photoKey":"${uploadTmpKey()}"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").isNumber)
            .andExpect(jsonPath("$.data.key").value(startsWith("memo/A1B2C3D4/")))
            .andExpect(jsonPath("$.data.url").exists())

        mockMvc
            .perform(get("/api/v1/memos/place-1").header("Authorization", bearer()))
            .andExpect(jsonPath("$.data.photos.length()").value(1))
            .andExpect(jsonPath("$.data.photos[0].key").value(startsWith("memo/A1B2C3D4/")))
    }

    @Test
    fun `DELETE로 사진을 지우면 GET에서 사라진다`() {
        val photoId =
            mockMvc
                .perform(
                    post("/api/v1/memos/place-1/photos")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"photoKey":"${uploadTmpKey()}"}"""),
                ).andReturn()
                .response
                .contentAsString
                .let { ObjectMapper().readTree(it).at("/data/id").asLong() }

        mockMvc
            .perform(delete("/api/v1/memos/place-1/photos/$photoId").header("Authorization", bearer()))
            .andExpect(status().isOk)

        mockMvc
            .perform(get("/api/v1/memos/place-1").header("Authorization", bearer()))
            .andExpect(jsonPath("$.data.photos.length()").value(0))
    }

    @Test
    fun `6장째 추가는 400 MEMO_TOO_MANY_PHOTOS`() {
        repeat(5) {
            mockMvc
                .perform(
                    post("/api/v1/memos/place-1/photos")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"photoKey":"${uploadTmpKey()}"}"""),
                ).andExpect(status().isOk)
        }

        mockMvc
            .perform(
                post("/api/v1/memos/place-1/photos")
                    .header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"photoKey":"${uploadTmpKey()}"}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("MEMO_TOO_MANY_PHOTOS"))
    }

    @Test
    fun `남의 사진 삭제는 404 MEMO_PHOTO_NOT_FOUND`() {
        val photoId =
            mockMvc
                .perform(
                    post("/api/v1/memos/place-1/photos")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"photoKey":"${uploadTmpKey()}"}"""),
                ).andReturn()
                .response
                .contentAsString
                .let { ObjectMapper().readTree(it).at("/data/id").asLong() }

        mockMvc
            .perform(delete("/api/v1/memos/place-1/photos/$photoId").header("Authorization", bearer("OTHER999")))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("MEMO_PHOTO_NOT_FOUND"))
    }
}
