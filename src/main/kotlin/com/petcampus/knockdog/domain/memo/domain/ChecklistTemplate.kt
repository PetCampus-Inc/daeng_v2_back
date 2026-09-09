package com.petcampus.knockdog.domain.memo.domain

class ChecklistTemplate(
    val code: String,
    val version: String,
    val locale: String,
    val title: String,
    val sections: List<ChecklistSection>,
) {
    private val questionByCode: Map<String, ChecklistQuestion> =
        sections.flatMap { it.questions }.associateBy { it.code }

    val questionCodes: Set<String> get() = questionByCode.keys

    fun question(code: String): ChecklistQuestion? = questionByCode[code]
}
