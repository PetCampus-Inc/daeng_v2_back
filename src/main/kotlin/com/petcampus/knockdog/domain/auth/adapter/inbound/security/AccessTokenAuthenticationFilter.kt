package com.petcampus.knockdog.domain.auth.adapter.inbound.security

import com.petcampus.knockdog.domain.auth.application.port.output.TokenPort
import com.petcampus.knockdog.global.exception.BusinessException
import com.petcampus.knockdog.global.exception.ErrorCode
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 커스텀 액세스 토큰 검증 필터 (ADR 0006 — `.oauth2Login()` 미사용, Spring Security는 SecurityContext 저장소로만 사용).
 * 토큰이 없거나 유효하지 않으면 그대로 통과시키고, 실제 접근 제어는 SecurityConfig의 authorizeHttpRequests가 담당한다.
 *
 * 검증 실패 사유(만료/무효)는 요청 속성에 남긴다. 프론트 인터셉터가 401 응답 본문의 `code`로
 * "토큰 갱신"과 "로그아웃"을 분기하기 때문에, 사유를 잃어버리면 만료된 토큰도 로그아웃으로 처리된다.
 */
@Component
class AccessTokenAuthenticationFilter(
    private val tokenPort: TokenPort,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        resolveToken(request)?.let { token ->
            try {
                val userCode = tokenPort.parseAccessTokenSubject(token)
                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(userCode.value, null, listOf(SimpleGrantedAuthority("ROLE_USER")))
            } catch (e: BusinessException) {
                SecurityContextHolder.clearContext()
                request.setAttribute(ERROR_CODE_ATTRIBUTE, e.errorCode)
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION) ?: return null
        return header.takeIf { it.startsWith(BEARER_PREFIX) }?.removePrefix(BEARER_PREFIX)
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "

        /** 토큰 검증 실패 사유. [AuthenticationFailureResponder]가 읽어 401 본문의 `code`로 내려준다. */
        const val ERROR_CODE_ATTRIBUTE = "knockdog.authErrorCode"

        fun resolveErrorCode(request: HttpServletRequest): ErrorCode? = request.getAttribute(ERROR_CODE_ATTRIBUTE) as? ErrorCode
    }
}
