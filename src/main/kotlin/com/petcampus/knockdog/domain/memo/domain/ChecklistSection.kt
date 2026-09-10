package com.petcampus.knockdog.domain.memo.domain

data class ChecklistSection(
    val code: String,
    val title: String,
    val questions: List<ChecklistQuestion>,
)
