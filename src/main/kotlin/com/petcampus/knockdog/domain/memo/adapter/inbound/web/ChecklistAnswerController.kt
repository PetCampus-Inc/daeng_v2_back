package com.petcampus.knockdog.domain.memo.adapter.inbound.web

import com.petcampus.knockdog.domain.memo.application.port.input.ChecklistAnswersView
import com.petcampus.knockdog.domain.memo.application.port.input.GetChecklistAnswersUseCase
import com.petcampus.knockdog.domain.memo.application.port.input.SaveChecklistAnswersCommand
import com.petcampus.knockdog.domain.memo.application.port.input.SaveChecklistAnswersUseCase
import com.petcampus.knockdog.global.response.Response
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/checklists")
class ChecklistAnswerController(
    private val getChecklistAnswersUseCase: GetChecklistAnswersUseCase,
    private val saveChecklistAnswersUseCase: SaveChecklistAnswersUseCase,
) {
    @GetMapping("/{targetId}")
    fun get(
        @AuthenticationPrincipal userCode: String,
        @PathVariable targetId: String,
    ): Response<ChecklistAnswersResponse> = Response.success(getChecklistAnswersUseCase.get(userCode, targetId).toResponse())

    @PutMapping("/{targetId}")
    fun save(
        @AuthenticationPrincipal userCode: String,
        @PathVariable targetId: String,
        @RequestBody request: SaveChecklistAnswersRequest,
    ): Response<ChecklistAnswersResponse> {
        val view =
            saveChecklistAnswersUseCase.save(
                SaveChecklistAnswersCommand(
                    userCode = userCode,
                    targetId = targetId,
                    answers = request.answers.map { SaveChecklistAnswersCommand.AnswerInput(it.questionId, it.value) },
                ),
            )
        return Response.success(view.toResponse())
    }
}

data class SaveChecklistAnswersRequest(
    val answers: List<Answer>,
) {
    data class Answer(
        val questionId: String,
        val value: String,
    )
}

data class ChecklistAnswersResponse(
    val sections: List<Section>,
) {
    data class Section(
        val sectionId: String,
        val title: String,
        val answers: List<Answer>,
    )

    data class Answer(
        val questionId: String,
        val question: String,
        val value: String,
    )
}

private fun ChecklistAnswersView.toResponse() =
    ChecklistAnswersResponse(
        sections =
            sections.map { section ->
                ChecklistAnswersResponse.Section(
                    sectionId = section.sectionId,
                    title = section.title,
                    answers = section.answers.map { ChecklistAnswersResponse.Answer(it.questionId, it.question, it.value) },
                )
            },
    )
