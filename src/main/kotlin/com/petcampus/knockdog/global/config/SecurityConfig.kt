package com.petcampus.knockdog.global.config

import com.petcampus.knockdog.domain.auth.adapter.inbound.security.AccessTokenAuthenticationFilter
import com.petcampus.knockdog.domain.auth.adapter.inbound.security.AuthenticationFailureResponder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/** ADR 0007: 공개 목록(permitAll)만 명시하고 나머지는 기본적으로 인증을 요구한다(기본 deny). */
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val accessTokenAuthenticationFilter: AccessTokenAuthenticationFilter,
    private val authenticationFailureResponder: AuthenticationFailureResponder,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { authorize ->
                authorize.requestMatchers(*PUBLIC_ENDPOINTS).permitAll()
                authorize.anyRequest().authenticated()
            }
            // 기본값은 인증 실패에 403 + Spring 기본 오류 본문을 내는데, 프론트 인터셉터가
            // 401에서만 토큰 갱신을 시도하므로 그대로 두면 액세스 토큰 만료가 복구되지 않는다.
            .exceptionHandling {
                it.authenticationEntryPoint(authenticationFailureResponder)
                it.accessDeniedHandler(authenticationFailureResponder)
            }.addFilterBefore(accessTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    companion object {
        // A-1~A-3은 로그인 이전 단계라 명시적으로 공개한다 (docs/domains/auth.md §3).
        // /api/v1/breeds는 로그인 여부와 무관한 품종 기준 데이터 조회이며(KD3-418),
        // 회원가입/반려견 등록 폼 등 비로그인 화면에서도 노출돼야 해 명시적으로 공개한다.
        // /error는 인증 실패가 아니라, 요청 처리 중 발생한 예외(400 등)가 내부적으로 forward되는 경로다 —
        // 여기를 permit하지 않으면 원래 에러 상태 코드가 403으로 가려진다(로컬 스모크 테스트로 확인됨).
        private val PUBLIC_ENDPOINTS =
            arrayOf(
                "/api/v1/auth/oidc-verifications",
                "/api/v1/breeds",
                "/api/v0/auth/login",
                "/api/v0/auth/refresh",
                "/api/v0/auth/logout",
                "/api/v1/users",
                "/error",
            )
    }
}
