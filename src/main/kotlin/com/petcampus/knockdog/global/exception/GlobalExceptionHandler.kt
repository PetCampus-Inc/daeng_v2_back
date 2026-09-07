package com.petcampus.knockdog.global.exception

import com.petcampus.knockdog.global.response.Response
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MissingRequestCookieException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

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

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParameter(e: MissingServletRequestParameterException): ResponseEntity<Response<Unit>> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Response.error(CommonErrorCode.INVALID_INPUT_VALUE, e.message))

    /**
     * 경로 변수·쿼리 파라미터의 타입 불일치(예: `Long` 자리에 숫자가 아닌 값). 이 핸들러가 없으면
     * catch-all(500)로 떨어진다 — Spring이 원래 자동으로 400 처리해주는 것을 이 클래스의 catch-all이
     * 가로채 버리기 때문에, 그 기본 동작을 되살리는 핸들러다. 메시지는 파라미터 타입 등 내부 정보 노출
     * 방지를 위해 고정 문구를 쓴다.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(e: MethodArgumentTypeMismatchException): ResponseEntity<Response<Unit>> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Response.error(CommonErrorCode.INVALID_INPUT_VALUE, "요청 경로 또는 파라미터 값이 올바르지 않습니다."))

    /**
     * 필수 쿠키 누락. 인증 토큰을 쿠키로 받는 API(`/api/v1/auth/login`, `/refresh`, `POST /api/v1/users`)를
     * 쿠키 없이 호출하면 발생한다. 클라이언트 실수이므로 500이 아니라 400으로 내린다.
     */
    @ExceptionHandler(MissingRequestCookieException::class)
    fun handleMissingCookie(e: MissingRequestCookieException): ResponseEntity<Response<Unit>> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Response.error(CommonErrorCode.INVALID_INPUT_VALUE, e.message))

    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun handleOptimisticLockingFailure(e: OptimisticLockingFailureException): ResponseEntity<Response<Unit>> =
        ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(Response.error(CommonErrorCode.RESOURCE_CONFLICT))

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
