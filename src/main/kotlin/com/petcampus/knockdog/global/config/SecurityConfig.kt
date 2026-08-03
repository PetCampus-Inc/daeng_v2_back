package com.petcampus.knockdog.global.config

import com.petcampus.knockdog.domain.auth.adapter.inbound.security.AccessTokenAuthenticationFilter
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
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { authorize ->
                authorize.requestMatchers(*PUBLIC_ENDPOINTS).permitAll()
                authorize.anyRequest().authenticated()
            }.addFilterBefore(accessTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    companion object {
        // A-1~A-3은 로그인 이전 단계라 명시적으로 공개한다 (docs/domains/auth.md §3).
        private val PUBLIC_ENDPOINTS =
            arrayOf(
                "/api/v1/auth/oidc-verifications",
                "/api/v1/auth/sessions",
                "/api/v1/users",
            )
    }
}
