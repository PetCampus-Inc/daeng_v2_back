package com.petcampus.knockdog.domain.pet.application.port.input

import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.pet.application.port.output.BreedSummary
import com.petcampus.knockdog.domain.pet.domain.Pet
import com.petcampus.knockdog.domain.pet.domain.PetId

interface GetPetUseCase {
    fun getPet(command: GetPetCommand): GetPetResult
}

data class GetPetCommand(
    val userCode: UserCode,
    val petId: PetId,
)

data class GetPetResult(
    val pet: Pet,
    val breed: BreedSummary,
)
