package com.petcampus.knockdog.domain.kindergarten.application.event

data class KindergartensComparedEvent(
    val userCode: String,
    val naverPlaceIds: List<String>,
)
