package com.petcampus.knockdog.domain.auth.adapter.outbound.jwt

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val issuer: String,
    val secretKey: String,
    val token: Token,
) {
    data class Token(
        val durations: Durations,
    )

    data class Durations(
        val oidcAuth: Long,
        val access: Long,
        val refresh: Long,
    )
}
