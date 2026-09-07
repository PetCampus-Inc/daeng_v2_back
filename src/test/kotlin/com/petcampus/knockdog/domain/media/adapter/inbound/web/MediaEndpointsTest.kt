package com.petcampus.knockdog.domain.media.adapter.inbound.web

import com.petcampus.knockdog.domain.auth.application.port.output.TokenPort
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.media.application.port.output.ObjectStoragePort
import com.petcampus.knockdog.domain.media.application.port.output.PresignedUrl
import com.petcampus.knockdog.domain.media.domain.MediaContentType
import com.petcampus.knockdog.domain.media.domain.ObjectKey
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@Import(MediaEndpointsTest.FakeStorageConfig::class)
class MediaEndpointsTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var tokenPort: TokenPort

    private fun bearer(userCode: String = "A1B2C3D4"): String = "Bearer " + tokenPort.issueAccessToken(UserCode(userCode))

    @Test
    fun `인증 없이 upload-url을 요청하면 401이다`() {
        mockMvc
            .perform(
                post("/api/v1/media/upload-urls")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"contentType":"image/webp"}"""),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `인증된 요청은 호출자 임시 네임스페이스 key와 presigned URL을 받는다`() {
        mockMvc
            .perform(
                post("/api/v1/media/upload-urls")
                    .header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"contentType":"image/webp"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.key").value(startsWith("tmp/A1B2C3D4/")))
            .andExpect(jsonPath("$.data.url").value(startsWith("https://fake/put/")))
            .andExpect(jsonPath("$.data.expiresIn").value(600))
    }

    @Test
    fun `지원하지 않는 content-type이면 400과 MEDIA_UNSUPPORTED_CONTENT_TYPE`() {
        mockMvc
            .perform(
                post("/api/v1/media/upload-urls")
                    .header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"contentType":"application/pdf"}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("MEDIA_UNSUPPORTED_CONTENT_TYPE"))
    }

    @Test
    fun `commit은 호출자 임시 네임스페이스가 아닌 key를 403으로 막는다`() {
        mockMvc
            .perform(
                post("/api/v1/media/commits")
                    .header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"key":"tmp/ZZZZZZZZ/x.webp","targetPath":"memo/1"}"""),
            ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("MEDIA_FORBIDDEN_KEY"))
    }

    @TestConfiguration
    class FakeStorageConfig {
        @Bean
        @Primary
        fun fakeObjectStoragePort(): ObjectStoragePort =
            object : ObjectStoragePort {
                private val objects = mutableSetOf<String>()

                override fun createUploadUrl(
                    key: ObjectKey,
                    contentType: MediaContentType,
                ): PresignedUrl {
                    objects.add(key.value)
                    return PresignedUrl("https://fake/put/${key.value}", expiresIn = 600)
                }

                override fun createDownloadUrl(key: ObjectKey) = PresignedUrl("https://fake/get/${key.value}", expiresIn = 300)

                override fun exists(key: ObjectKey) = objects.contains(key.value)

                override fun copy(
                    source: ObjectKey,
                    destination: ObjectKey,
                ) {
                    objects.add(destination.value)
                }

                override fun delete(key: ObjectKey) {
                    objects.remove(key.value)
                }
            }
    }
}
