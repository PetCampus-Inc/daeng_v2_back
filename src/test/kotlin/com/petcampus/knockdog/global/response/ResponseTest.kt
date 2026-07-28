package com.petcampus.knockdog.global.response

import com.petcampus.knockdog.global.exception.CommonErrorCode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResponseTest {
    @Test
    fun `success는 status 200과 data를 담는다`() {
        val response = Response.success(data = "hello")

        assertEquals(200, response.status)
        assertEquals("hello", response.data)
        assertNull(response.code)
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
