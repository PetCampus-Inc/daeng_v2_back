package com.petcampus.knockdog.domain.auth.adapter.outbound.oidc

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.petcampus.knockdog.domain.auth.application.AuthErrorCode
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.stereotype.Component
import java.util.Base64

@Component
class JwtHeaderParser(
    private val objectMapper: ObjectMapper,
) {
    fun parseHeaders(token: String): Map<String, String> {
        val headerSegment = token.split(".").firstOrNull() ?: throw BusinessException(AuthErrorCode.INVALID_TOKEN)

        return try {
            objectMapper.readValue(Base64.getUrlDecoder().decode(headerSegment))
        } catch (e: Exception) {
            throw BusinessException(AuthErrorCode.INVALID_TOKEN)
        }
    }
}
