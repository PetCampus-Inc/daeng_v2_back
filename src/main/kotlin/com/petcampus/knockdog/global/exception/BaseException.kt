package com.petcampus.knockdog.global.exception

import org.springframework.http.HttpStatus

/** 도메인별 예외가 공통으로 상속하는 예외. GlobalExceptionHandler가 status를 그대로 응답에 반영한다. */
open class BaseException(
    val status: HttpStatus,
    message: String,
) : RuntimeException(message)
