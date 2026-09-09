package com.petcampus.knockdog.domain.memo.application.port.input

interface GetChecklistAnswersUseCase {
    fun get(
        userCode: String,
        targetId: String,
    ): ChecklistAnswersView
}

data class ChecklistAnswersView(
    val sections: List<SectionAnswers>,
) {
    data class SectionAnswers(
        val sectionId: String,
        val title: String,
        val answers: List<AnswerItem>,
    )

    data class AnswerItem(
        val questionId: String,
        val question: String,
        val value: String,
    )
}
