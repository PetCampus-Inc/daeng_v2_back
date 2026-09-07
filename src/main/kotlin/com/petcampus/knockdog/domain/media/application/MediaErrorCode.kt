package com.petcampus.knockdog.domain.media.application

import com.petcampus.knockdog.global.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class MediaErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    UNSUPPORTED_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "MEDIA_UNSUPPORTED_CONTENT_TYPE", "지원하지 않는 이미지 형식입니다."),
    OBJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "MEDIA_OBJECT_NOT_FOUND", "대상 이미지를 찾을 수 없습니다."),
    FORBIDDEN_KEY(HttpStatus.FORBIDDEN, "MEDIA_FORBIDDEN_KEY", "접근할 수 없는 이미지 경로입니다."),
    INVALID_TARGET_PATH(HttpStatus.BAD_REQUEST, "MEDIA_INVALID_TARGET_PATH", "잘못된 저장 경로입니다."),
}
