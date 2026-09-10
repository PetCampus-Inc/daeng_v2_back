package com.petcampus.knockdog.domain.memo.adapter.outbound.template

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.petcampus.knockdog.domain.memo.application.port.output.LoadChecklistTemplatePort
import com.petcampus.knockdog.domain.memo.domain.ChecklistQuestion
import com.petcampus.knockdog.domain.memo.domain.ChecklistQuestionType
import com.petcampus.knockdog.domain.memo.domain.ChecklistSection
import com.petcampus.knockdog.domain.memo.domain.ChecklistTemplate
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

@Component
class ChecklistTemplateResourceAdapter : LoadChecklistTemplatePort {
    private val template: ChecklistTemplate = parse()

    override fun load(): ChecklistTemplate = template

    private fun parse(): ChecklistTemplate {
        val raw: RawTemplate = ClassPathResource(RESOURCE_PATH).inputStream.use { objectMapper.readValue(it) }
        return ChecklistTemplate(
            code = raw.code,
            version = raw.version,
            locale = raw.locale,
            title = raw.title,
            sections =
                raw.sections.map { section ->
                    ChecklistSection(
                        code = section.code,
                        title = section.title,
                        questions =
                            section.questions.map {
                                ChecklistQuestion(
                                    code = it.code,
                                    label = it.label,
                                    type = ChecklistQuestionType.valueOf(it.type),
                                    min = it.min,
                                    max = it.max,
                                )
                            },
                    )
                },
        )
    }

    private data class RawTemplate(
        val code: String,
        val version: String,
        val locale: String,
        val title: String,
        val sections: List<RawSection>,
    )

    private data class RawSection(
        val code: String,
        val title: String,
        val questions: List<RawQuestion>,
    )

    private data class RawQuestion(
        val code: String,
        val label: String,
        val type: String,
        val min: Int? = null,
        val max: Int? = null,
    )

    companion object {
        private const val RESOURCE_PATH = "checklists/registration.ko-KR.json"
        private val objectMapper =
            jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
}
