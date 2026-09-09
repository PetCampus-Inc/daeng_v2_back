package com.petcampus.knockdog.domain.memo.application.port.input

import java.time.LocalDate

interface GetMemoedKindergartensUseCase {
    fun list(userCode: String): List<MemoedKindergartenView>
}

data class MemoedKindergartenView(
    val shopId: String,
    val content: String?,
    val memoDate: LocalDate,
)
