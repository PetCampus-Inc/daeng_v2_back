package com.petcampus.knockdog.domain.auth.application

import com.petcampus.knockdog.global.exception.ErrorCode
import org.springframework.http.HttpStatus

/**
 * 레거시(`knockdog_server`)의 `AuthErrorCode.java`와 동일한 `code` 문자열을 유지한다 —
 * 프론트가 이미 이 값으로 분기하고 있어 임의로 바꿀 수 없다 (docs/conventions/error-handling.md §1).
 */
enum class AuthErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    INVALID_PROVIDER(HttpStatus.BAD_REQUEST, "INVALID_PROVIDER", "지원하지 않는 OAuth Provider입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 인증 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_TOKEN", "인증 토큰이 만료되었습니다."),
    TOKEN_VERIFICATION_FAILED(HttpStatus.UNAUTHORIZED, "TOKEN_VERIFICATION_FAILED", "토큰 검증에 실패했습니다."),
    EXTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "EXTERNAL_SERVER_ERROR", "외부 서버 오류가 발생했습니다."),
    ALREADY_LINKED_USER(HttpStatus.CONFLICT, "ALREADY_LINKED_USER", "이미 연동된 회원입니다."),
    USER_NOT_LINKED(HttpStatus.FORBIDDEN, "USER_NOT_LINKED", "소셜 계정과 연동되지 않은 회원입니다."),
    REJOINING_RESTRICTION_PERIOD(HttpStatus.FORBIDDEN, "REJOINING_RESTRICTION_PERIOD", "탈퇴 후 재가입 제한 기간입니다."),
    WITHDRAWN_USER(HttpStatus.FORBIDDEN, "WITHDRAWN_USER", "탈퇴 처리 된 회원입니다."),
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "NOT_FOUND_USER", "존재하지 않는 회원입니다."),
    NOT_FOUND_SOCIAL_USER(HttpStatus.NOT_FOUND, "NOT_FOUND_SOCIAL_USER", "존재하지 않는 소셜 계정입니다."),

    REQUIRED_AGREEMENT_NOT_COMPLETED(
        HttpStatus.BAD_REQUEST,
        "REQUIRED_AGREEMENT_NOT_COMPLETED",
        "필수 약관에 모두 동의해야 합니다.",
    ),

    /** 레거시엔 없던 신규 규칙 — PENDING 상태 소셜 계정의 회원가입 우회를 막는다 (KD3-258 리뷰 반영). */
    PENDING_SOCIAL_USER(HttpStatus.CONFLICT, "PENDING_SOCIAL_USER", "다른 소셜 계정으로 이미 가입된 이메일입니다. 재연동이 필요합니다."),
}
