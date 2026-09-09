package com.petcampus.knockdog.domain.pet.application.port.input

import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.pet.application.port.output.BreedSummary
import com.petcampus.knockdog.domain.pet.domain.Gender
import com.petcampus.knockdog.domain.pet.domain.Pet
import com.petcampus.knockdog.domain.pet.domain.Relationship

interface CreatePetUseCase {
    fun create(command: CreatePetCommand): CreatePetResult
}

data class CreatePetCommand(
    val userCode: UserCode,
    val name: String,
    val profileImage: String?,
    val relationship: Relationship,
    val relationshipText: String?,
    val breedId: Long,
    val gender: Gender,
    val birthYear: Int?,
    val weight: Double,
    val isNeutered: Boolean?,
)

data class CreatePetResult(
    val pet: Pet,
    val breed: BreedSummary,
)
