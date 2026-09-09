package com.petcampus.knockdog.domain.pet.application.port.input

import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.pet.application.port.output.BreedSummary
import com.petcampus.knockdog.domain.pet.domain.Pet

interface GetPetsUseCase {
    fun getPets(command: GetPetsCommand): GetPetsResult
}

data class GetPetsCommand(
    val userCode: UserCode,
)

data class GetPetsResult(
    val pets: List<PetWithBreed>,
)

data class PetWithBreed(
    val pet: Pet,
    val breed: BreedSummary,
)
