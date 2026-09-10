package com.petcampus.knockdog.domain.comparison.application

import com.petcampus.knockdog.global.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class ComparisonHistoryErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMPARISON_HISTORY_NOT_FOUND", "비교 히스토리를 찾을 수 없습니다."),
    NOT_OWNER(HttpStatus.FORBIDDEN, "COMPARISON_HISTORY_NOT_OWNER", "본인의 비교 히스토리가 아닙니다."),
}
