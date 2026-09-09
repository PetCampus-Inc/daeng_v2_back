package com.petcampus.knockdog.domain.memo.application.port.input

interface GetChecklistTemplateUseCase {
    fun getTemplate(): ChecklistTemplateView
}

data class ChecklistTemplateView(
    val template: Meta,
    val sections: List<SectionView>,
) {
    data class Meta(
        val code: String,
        val version: String,
        val locale: String,
        val title: String,
    )

    data class SectionView(
        val id: String,
        val title: String,
        val questions: List<QuestionView>,
    )

    data class QuestionView(
        val id: String,
        val label: String,
        val type: String,
    )
}
