package com.petcampus.knockdog.domain.memo.application.port.input

interface SaveChecklistAnswersUseCase {
    fun save(command: SaveChecklistAnswersCommand): ChecklistAnswersView
}

data class SaveChecklistAnswersCommand(
    val userCode: String,
    val targetId: String,
    val answers: List<AnswerInput>,
) {
    data class AnswerInput(
        val questionId: String,
        val value: String,
    )
}
