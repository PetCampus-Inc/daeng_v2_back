package com.petcampus.knockdog.domain.pet.application

import com.petcampus.knockdog.global.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class PetErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "PET-404-1", "해당 강아지가 존재하지 않습니다."),
    NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "PET-403-1", "해당 강아지에 접근할 권한이 없습니다."),
    LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "PET-400-1", "강아지는 최대 5마리까지 등록할 수 있어요."),
    RELATIONSHIP_TEXT_REQUIRED(HttpStatus.BAD_REQUEST, "PET-400-2", "관계를 직접 입력해 주세요."),
    NOT_FOUND_BREED(HttpStatus.BAD_REQUEST, "PET-400-3", "존재하지 않는 견종입니다."),
}
