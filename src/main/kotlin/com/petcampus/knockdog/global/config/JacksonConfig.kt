package com.petcampus.knockdog.global.config

import org.openapitools.jackson.nullable.JsonNullableModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig {
    @Bean
    fun jsonNullableModule(): JsonNullableModule = JsonNullableModule()
}
