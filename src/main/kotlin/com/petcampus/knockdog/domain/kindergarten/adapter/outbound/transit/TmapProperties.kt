package com.petcampus.knockdog.domain.kindergarten.adapter.outbound.transit

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tmap.api")
data class TmapProperties(
    val key: String,
    val baseUrl: String,
    val cacheTtlDays: Long = 7,
)
