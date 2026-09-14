package com.petcampus.knockdog.domain.kindergarten.adapter.outbound.travel

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tmap.api")
data class TmapProperties(
    val key: String,
    val baseUrl: String,
    val cacheTtlDays: Long = 7,
)
