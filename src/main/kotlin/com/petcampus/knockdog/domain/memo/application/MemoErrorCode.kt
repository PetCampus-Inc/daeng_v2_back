package com.petcampus.knockdog.domain.memo.application

import com.petcampus.knockdog.global.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class MemoErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "MEMO_CONTENT_TOO_LONG", "메모는 2000자 이내로 작성해 주세요."),
    TOO_MANY_PHOTOS(HttpStatus.BAD_REQUEST, "MEMO_TOO_MANY_PHOTOS", "사진은 최대 5장까지 등록할 수 있습니다."),
    INVALID_PHOTO_KEY(HttpStatus.BAD_REQUEST, "MEMO_INVALID_PHOTO_KEY", "올바르지 않은 사진입니다."),
    PHOTO_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMO_PHOTO_NOT_FOUND", "사진을 찾을 수 없습니다."),
    INVALID_CHECKLIST_ANSWER(HttpStatus.BAD_REQUEST, "MEMO_INVALID_CHECKLIST_ANSWER", "체크리스트 답변 형식이 올바르지 않습니다."),
}
