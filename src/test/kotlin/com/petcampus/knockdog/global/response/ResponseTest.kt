package com.petcampus.knockdog.global.response

import com.petcampus.knockdog.global.exception.CommonErrorCode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResponseTest {
    /** 레거시가 성공에도 `code: "SUCCESS"`를 내리고 프론트가 그 값으로 분기하는 곳이 있다. */
    @Test
    fun `success는 status 200과 data와 SUCCESS 코드를 담는다`() {
        val response = Response.success(data = "hello")

        assertEquals(200, response.status)
        assertEquals("hello", response.data)
        assertEquals("SUCCESS", response.code)
    }

    /** 이메일 인증처럼 성공에도 결과를 구분해야 하는 API는 코드를 직접 넘긴다(레거시 동작). */
    @Test
    fun `success에 결과 코드를 직접 넘길 수 있다`() {
        val response = Response.success(data = null, message = "이미 인증된 이메일입니다.", code = "ALREADY_VERIFIED")

        assertEquals(200, response.status)
        assertEquals("ALREADY_VERIFIED", response.code)
        assertEquals("이미 인증된 이메일입니다.", response.message)
    }

    @Test
    fun `error는 ErrorCode의 status와 code를 그대로 담는다`() {
        val response = Response.error(CommonErrorCode.INVALID_INPUT_VALUE)

        assertEquals(CommonErrorCode.INVALID_INPUT_VALUE.status.value(), response.status)
        assertEquals(CommonErrorCode.INVALID_INPUT_VALUE.code, response.code)
        assertEquals(CommonErrorCode.INVALID_INPUT_VALUE.message, response.message)
        assertNull(response.data)
    }
}
