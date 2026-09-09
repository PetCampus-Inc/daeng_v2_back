package com.petcampus.knockdog.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime
import kotlin.test.assertEquals

@SpringBootTest
class ResponseDateTimeWiringTest {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private data class Sample(
        val at: LocalDateTime,
    )

    @Test
    fun `애플리케이션 ObjectMapper가 LocalDateTime을 초 단위 문자열로 직렬화한다`() {
        val json = objectMapper.writeValueAsString(Sample(LocalDateTime.of(2026, 9, 5, 11, 38, 48, 123_456_789)))

        assertEquals("""{"at":"2026-09-05T11:38:48"}""", json)
    }
}
