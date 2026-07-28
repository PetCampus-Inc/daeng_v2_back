package com.petcampus.knockdog.global.exception

import com.petcampus.knockdog.global.response.Response
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<Response<Unit>> =
        ResponseEntity.status(e.errorCode.status).body(Response.error(e.errorCode, e.message))

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<Response<Unit>> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Response.error(CommonErrorCode.INVALID_INPUT_VALUE, e.message))

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(e: NoSuchElementException): ResponseEntity<Response<Unit>> =
        ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Response.error(CommonErrorCode.RESOURCE_NOT_FOUND, e.message))

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMessageNotReadable(e: HttpMessageNotReadableException): ResponseEntity<Response<Unit>> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Response.error(CommonErrorCode.INVALID_INPUT_VALUE, "요청 본문을 읽을 수 없습니다."))

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(e: HttpRequestMethodNotSupportedException): ResponseEntity<Response<Unit>> =
        ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(Response.error(CommonErrorCode.METHOD_NOT_ALLOWED, e.message))

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<Response<Unit>> =
        ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Response.error(CommonErrorCode.INTERNAL_SERVER_ERROR))
}
