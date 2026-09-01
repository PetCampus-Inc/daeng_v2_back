package com.petcampus.knockdog.domain.auth.adapter.inbound.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.petcampus.knockdog.domain.auth.application.AuthErrorCode
import com.petcampus.knockdog.global.response.Response
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

/**
 * 인증 실패를 **401 + `Response` 규격**으로 내려준다.
 *
 * Spring Security 기본 동작은 익명 요청에 `AccessDeniedException`(403)을 내고 본문도 Spring 기본
 * 오류 형식이다. 그러면 프론트 인터셉터가 동작하지 않는다 — `tokenRefreshInterceptor`는
 * `status === 401`일 때만 진입하고, 본문의 `code`로 갱신/로그아웃을 분기하기 때문이다.
 * 레거시 `TokenAuthenticationFilter`도 인증 실패를 401 + 에러 코드 JSON으로 응답했다.
 */
@Component
class AuthenticationFailureResponder(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint,
    AccessDeniedHandler {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) = write(request, response)

    /**
     * 익명 요청은 Spring Security가 `AccessDeniedException`으로 넘기기도 한다.
     * 인증 자체가 없는 상태이므로 403이 아니라 401로 통일한다.
     */
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) = write(request, response)

    private fun write(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        // 토큰이 만료된 것과 아예 없는 것은 프론트 처리가 다르다(갱신 vs 로그아웃).
        val errorCode = AccessTokenAuthenticationFilter.resolveErrorCode(request) ?: AuthErrorCode.UNAUTHORIZED_REQUEST

        response.status = errorCode.status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = StandardCharsets.UTF_8.name()
        response.writer.write(objectMapper.writeValueAsString(Response.error(errorCode)))
    }
}
