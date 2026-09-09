package com.petcampus.knockdog.global.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Configuration
class JacksonDateTimeConfig {
    @Bean
    fun responseDateTimeFormatCustomizer(): Jackson2ObjectMapperBuilderCustomizer =
        Jackson2ObjectMapperBuilderCustomizer { builder ->
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            builder.serializers(
                LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)),
                LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_PATTERN)),
                MinutePrecisionLocalTimeSerializer,
            )
        }

    private object MinutePrecisionLocalTimeSerializer : StdSerializer<LocalTime>(LocalTime::class.java) {
        private val hourMinute = DateTimeFormatter.ofPattern("HH:mm")
        private val hourMinuteSecond = DateTimeFormatter.ofPattern("HH:mm:ss")

        override fun serialize(
            value: LocalTime,
            generator: JsonGenerator,
            provider: SerializerProvider,
        ) {
            val truncated = value.truncatedTo(ChronoUnit.SECONDS)
            val formatter = if (truncated.second == 0) hourMinute else hourMinuteSecond
            generator.writeString(truncated.format(formatter))
        }
    }

    companion object {
        private const val DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"
        private const val DATE_PATTERN = "yyyy-MM-dd"
    }
}
