package com.petcampus.knockdog.domain.breed.domain

data class Breed(
    val id: Long,
    val displayOrder: Int,
    val fciStandardNumber: Int?,
    val nameEn: String,
    val nameKo: String,
    val alias: String?,
)
