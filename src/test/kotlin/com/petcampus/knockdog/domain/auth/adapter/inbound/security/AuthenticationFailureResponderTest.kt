package com.petcampus.knockdog.domain.auth.adapter.inbound.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.petcampus.knockdog.domain.auth.application.AuthErrorCode
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.InsufficientAuthenticationException
import kotlin.test.assertEquals

/**
 * 프론트 인터셉터는 `status === 401`일 때만 토큰 갱신에 진입하고, 본문의 `code`로
 * 갱신(`EXPIRED_TOKEN`)과 로그아웃(그 외)을 분기한다. 이 두 계약을 고정한다.
 */
class AuthenticationFailureResponderTest {
    private val objectMapper = ObjectMapper()
    private val responder = AuthenticationFailureResponder(objectMapper)

    private fun bodyOf(response: MockHttpServletResponse) = objectMapper.readTree(response.contentAsString)

    @Test
    fun `토큰이 없으면 401 UNAUTHORIZED_REQUEST로 응답한다`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        responder.commence(request, response, InsufficientAuthenticationException("no token"))

        assertEquals(401, response.status)
        assertEquals("UNAUTHORIZED_REQUEST", bodyOf(response)["code"].asText())
    }

    @Test
    fun `만료된 토큰이면 401 EXPIRED_TOKEN으로 응답한다`() {
        val request = MockHttpServletRequest()
        request.setAttribute(AccessTokenAuthenticationFilter.ERROR_CODE_ATTRIBUTE, AuthErrorCode.EXPIRED_TOKEN)
        val response = MockHttpServletResponse()

        responder.commence(request, response, InsufficientAuthenticationException("expired"))

        assertEquals(401, response.status)
        assertEquals("EXPIRED_TOKEN", bodyOf(response)["code"].asText())
    }

    @Test
    fun `유효하지 않은 토큰이면 401 INVALID_TOKEN으로 응답한다`() {
        val request = MockHttpServletRequest()
        request.setAttribute(AccessTokenAuthenticationFilter.ERROR_CODE_ATTRIBUTE, AuthErrorCode.INVALID_TOKEN)
        val response = MockHttpServletResponse()

        responder.commence(request, response, InsufficientAuthenticationException("invalid"))

        assertEquals(401, response.status)
        assertEquals("INVALID_TOKEN", bodyOf(response)["code"].asText())
    }

    /** 익명 요청이 AccessDeniedException으로 넘어와도 403이 아니라 401이어야 한다. */
    @Test
    fun `AccessDenied 경로도 401로 통일한다`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        responder.handle(request, response, AccessDeniedException("denied"))

        assertEquals(401, response.status)
        assertEquals("UNAUTHORIZED_REQUEST", bodyOf(response)["code"].asText())
    }

    @Test
    fun `응답 본문은 Response 규격을 따른다`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        responder.commence(request, response, InsufficientAuthenticationException("no token"))

        val body = bodyOf(response)
        assertEquals(401, body["status"].asInt())
        assertEquals(AuthErrorCode.UNAUTHORIZED_REQUEST.message, body["message"].asText())
    }
}
