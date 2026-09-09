package com.petcampus.knockdog.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.assertEquals

class ResponseDateTimeFormatTest {
    private data class DateTimeSample(
        val at: LocalDateTime,
        val on: LocalDate,
    )

    private data class TimeSample(
        val time: LocalTime,
    )

    private val objectMapper: ObjectMapper =
        Jackson2ObjectMapperBuilder()
            .also { JacksonDateTimeConfig().responseDateTimeFormatCustomizer().customize(it) }
            .build()

    @Test
    fun `LocalDateTime은 나노초가 있어도 초 단위까지만 오프셋 없는 ISO-8601 문자열로 나간다`() {
        val json =
            objectMapper.writeValueAsString(
                DateTimeSample(LocalDateTime.of(2026, 9, 5, 11, 38, 48, 123_456_789), LocalDate.of(2026, 9, 5)),
            )

        assertEquals("""{"at":"2026-09-05T11:38:48","on":"2026-09-05"}""", json)
    }

    @Test
    fun `LocalDateTime은 초가 0이어도 소수점 없이 초까지 표기한다`() {
        val json =
            objectMapper.writeValueAsString(
                DateTimeSample(LocalDateTime.of(2026, 1, 2, 3, 4, 0), LocalDate.of(2026, 1, 2)),
            )

        assertEquals("""{"at":"2026-01-02T03:04:00","on":"2026-01-02"}""", json)
    }

    @Test
    fun `LocalTime은 초가 0이면 HH_mm으로 나간다`() {
        val json = objectMapper.writeValueAsString(TimeSample(LocalTime.of(17, 59, 0)))

        assertEquals("""{"time":"17:59"}""", json)
    }

    @Test
    fun `LocalTime은 초가 있으면 HH_mm_ss로 나간다`() {
        val json = objectMapper.writeValueAsString(TimeSample(LocalTime.of(17, 59, 30)))

        assertEquals("""{"time":"17:59:30"}""", json)
    }

    @Test
    fun `LocalTime은 나노초를 버린다`() {
        val json = objectMapper.writeValueAsString(TimeSample(LocalTime.of(17, 59, 30, 123_456_789)))

        assertEquals("""{"time":"17:59:30"}""", json)
    }
}
