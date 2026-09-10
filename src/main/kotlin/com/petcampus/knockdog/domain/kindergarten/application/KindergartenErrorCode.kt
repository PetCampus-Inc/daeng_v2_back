package com.petcampus.knockdog.domain.kindergarten.application

import com.petcampus.knockdog.global.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class KindergartenErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    COMPARISON_TARGET_COUNT(HttpStatus.BAD_REQUEST, "COMPARISON_TARGET_COUNT", "비교할 유치원은 2곳이어야 합니다."),
    COMPARISON_TARGET_DUPLICATED(HttpStatus.BAD_REQUEST, "COMPARISON_TARGET_DUPLICATED", "비교할 유치원이 중복되었습니다."),
}
