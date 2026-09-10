package com.petcampus.knockdog.domain.memo.adapter.outbound.persistence

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class ChecklistAnswersJsonConverter : AttributeConverter<Map<String, String>, String> {
    override fun convertToDatabaseColumn(attribute: Map<String, String>?): String =
        objectMapper.writeValueAsString(attribute ?: emptyMap<String, String>())

    override fun convertToEntityAttribute(dbData: String?): Map<String, String> =
        if (dbData.isNullOrBlank()) emptyMap() else objectMapper.readValue(dbData)

    companion object {
        private val objectMapper = jacksonObjectMapper()
    }
}
