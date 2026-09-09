package com.petcampus.knockdog.domain.memo.domain

data class ChecklistQuestion(
    val code: String,
    val label: String,
    val type: ChecklistQuestionType,
    val min: Int?,
    val max: Int?,
) {
    fun normalize(rawValue: String): String? =
        when (type) {
            ChecklistQuestionType.TRI_STATE -> normalizeTriState(rawValue)
            ChecklistQuestionType.INTEGER -> normalizeInteger(rawValue)
        }

    private fun normalizeTriState(rawValue: String): String? =
        when (rawValue.trim().uppercase()) {
            "YES" -> "YES"
            "NO" -> "NO"
            "UNKNOWN" -> "UNKNOWN"
            else -> null
        }

    private fun normalizeInteger(rawValue: String): String? {
        val number = rawValue.trim().toIntOrNull() ?: return null
        if (min != null && number < min) return null
        if (max != null && number > max) return null
        return number.toString()
    }
}
