package com.petcampus.knockdog.domain.kindergarten.adapter.outbound.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.time.DayOfWeek

@Converter
class DayOfWeekListConverter : AttributeConverter<List<DayOfWeek>, String> {
    override fun convertToDatabaseColumn(attribute: List<DayOfWeek>?): String =
        objectMapper.writeValueAsString(attribute?.map { it.name } ?: emptyList<String>())

    override fun convertToEntityAttribute(dbData: String?): List<DayOfWeek> =
        if (dbData.isNullOrBlank()) {
            emptyList()
        } else {
            objectMapper.readValue(dbData, Array<String>::class.java).map { DayOfWeek.valueOf(it) }
        }

    companion object {
        private val objectMapper = ObjectMapper()
    }
}
