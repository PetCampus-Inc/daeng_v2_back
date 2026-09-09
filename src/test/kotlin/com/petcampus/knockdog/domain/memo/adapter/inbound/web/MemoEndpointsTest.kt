package com.petcampus.knockdog.domain.memo.adapter.inbound.web

import com.petcampus.knockdog.domain.auth.application.port.output.TokenPort
import com.petcampus.knockdog.domain.auth.domain.UserCode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemoEndpointsTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var tokenPort: TokenPort

    private fun bearer(userCode: String = "A1B2C3D4") = "Bearer " + tokenPort.issueAccessToken(UserCode(userCode))

    @Test
    fun `인증 없이 조회하면 401`() {
        mockMvc.perform(get("/api/v1/memos/place-1")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `메모가 없으면 200에 content null, photos 빈 배열`() {
        mockMvc
            .perform(get("/api/v1/memos/place-1").header("Authorization", bearer()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").doesNotExist())
            .andExpect(jsonPath("$.data.photos").isArray)
            .andExpect(jsonPath("$.data.photos.length()").value(0))
    }

    @Test
    fun `PUT으로 저장하고 GET하면 같은 content`() {
        mockMvc
            .perform(
                put("/api/v1/memos/place-1")
                    .header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"content":"우리 뽀삐 메모"}"""),
            ).andExpect(status().isOk)

        mockMvc
            .perform(get("/api/v1/memos/place-1").header("Authorization", bearer()))
            .andExpect(jsonPath("$.data.content").value("우리 뽀삐 메모"))
    }

    @Test
    fun `content가 2000자를 넘으면 400 MEMO_CONTENT_TOO_LONG`() {
        val body = "{\"content\":\"" + "가".repeat(2001) + "\"}"

        mockMvc
            .perform(
                put("/api/v1/memos/place-1")
                    .header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("MEMO_CONTENT_TOO_LONG"))
    }

    @Test
    fun `GET memos는 내 유치원별 메모 목록을 shopId와 memoDate와 함께 준다`() {
        mockMvc
            .perform(
                put("/api/v1/memos/place-1")
                    .header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"content":"a"}"""),
            ).andExpect(status().isOk)

        mockMvc
            .perform(get("/api/v1/memos").header("Authorization", bearer()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.memos[0].shopId").value("place-1"))
            .andExpect(jsonPath("$.data.memos[0].memoDate").exists())
    }
}
