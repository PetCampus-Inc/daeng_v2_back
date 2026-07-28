package com.petcampus.knockdog.global.exception

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 컨트롤러 어드바이스는 Spring 컨텍스트 없이도 순수 함수처럼 검증할 수 있다.
 */
class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler()

    @Test
    fun `BusinessException은 ErrorCode의 status와 code를 그대로 응답한다`() {
        val exception = BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND)

        val response = handler.handleBusinessException(exception)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(HttpStatus.NOT_FOUND.value(), response.body?.status)
        assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND.code, response.body?.code)
        assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND.message, response.body?.message)
    }

    @Test
    fun `BusinessException에 커스텀 메시지를 주면 ErrorCode 기본 메시지 대신 사용된다`() {
        val exception = BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "id는 필수입니다.")

        val response = handler.handleBusinessException(exception)

        assertEquals("id는 필수입니다.", response.body?.message)
    }

    @Test
    fun `IllegalArgumentException은 400과 함께 Response 포맷으로 응답한다`() {
        val response = handler.handleIllegalArgument(IllegalArgumentException("잘못된 값입니다."))

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(CommonErrorCode.INVALID_INPUT_VALUE.code, response.body?.code)
        assertEquals("잘못된 값입니다.", response.body?.message)
    }

    @Test
    fun `NoSuchElementException은 404와 함께 Response 포맷으로 응답한다`() {
        val response = handler.handleNotFound(NoSuchElementException("회원을 찾을 수 없습니다."))

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND.code, response.body?.code)
        assertEquals("회원을 찾을 수 없습니다.", response.body?.message)
    }

    @Test
    fun `매핑되지 않은 예외는 500과 공통 에러 메시지로 응답한다`() {
        val response = handler.handleException(RuntimeException("아무 예외"))

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals(CommonErrorCode.INTERNAL_SERVER_ERROR.code, response.body?.code)
        assertEquals(CommonErrorCode.INTERNAL_SERVER_ERROR.message, response.body?.message)
        assertNull(response.body?.data)
    }
}
