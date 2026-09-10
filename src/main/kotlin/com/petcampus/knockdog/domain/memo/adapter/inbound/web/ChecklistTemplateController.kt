package com.petcampus.knockdog.domain.memo.adapter.inbound.web

import com.petcampus.knockdog.domain.memo.application.port.input.ChecklistTemplateView
import com.petcampus.knockdog.domain.memo.application.port.input.GetChecklistTemplateUseCase
import com.petcampus.knockdog.global.response.Response
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/checklists")
class ChecklistTemplateController(
    private val getChecklistTemplateUseCase: GetChecklistTemplateUseCase,
) {
    @GetMapping("/template")
    fun getTemplate(): Response<ChecklistTemplateResponse> = Response.success(getChecklistTemplateUseCase.getTemplate().toResponse())
}

data class ChecklistTemplateResponse(
    val template: Meta,
    val sections: List<Section>,
) {
    data class Meta(
        val code: String,
        val version: String,
        val locale: String,
        val title: String,
    )

    data class Section(
        val id: String,
        val title: String,
        val questions: List<Question>,
    )

    data class Question(
        val id: String,
        val label: String,
        val type: String,
    )
}

private fun ChecklistTemplateView.toResponse() =
    ChecklistTemplateResponse(
        template = ChecklistTemplateResponse.Meta(template.code, template.version, template.locale, template.title),
        sections =
            sections.map { section ->
                ChecklistTemplateResponse.Section(
                    id = section.id,
                    title = section.title,
                    questions = section.questions.map { ChecklistTemplateResponse.Question(it.id, it.label, it.type) },
                )
            },
    )
