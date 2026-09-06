package com.petcampus.knockdog.domain.pet.application.port.output

interface LoadBreedPort {
    fun findById(breedId: Long): BreedSummary?
}

data class BreedSummary(
    val id: Long,
    val nameKo: String,
    val alias: String?,
)
