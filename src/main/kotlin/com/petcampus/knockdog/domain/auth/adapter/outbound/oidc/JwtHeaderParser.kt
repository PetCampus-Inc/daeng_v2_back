package com.petcampus.knockdog.domain.auth.adapter.outbound.oidc

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.petcampus.knockdog.domain.auth.application.AuthException
import org.springframework.stereotype.Component
import java.util.Base64

@Component
class JwtHeaderParser(
    private val objectMapper: ObjectMapper,
) {
    fun parseHeaders(token: String): Map<String, String> {
        val headerSegment = token.split(".").firstOrNull() ?: throw AuthException.invalidToken()

        return try {
            objectMapper.readValue(Base64.getUrlDecoder().decode(headerSegment))
        } catch (e: Exception) {
            throw AuthException.invalidToken()
        }
    }
}
