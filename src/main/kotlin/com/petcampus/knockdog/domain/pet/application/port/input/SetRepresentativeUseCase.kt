package com.petcampus.knockdog.domain.pet.application.port.input

import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.pet.application.port.output.BreedSummary
import com.petcampus.knockdog.domain.pet.domain.Pet
import com.petcampus.knockdog.domain.pet.domain.PetId

interface SetRepresentativeUseCase {
    fun setRepresentative(command: SetRepresentativeCommand): SetRepresentativeResult
}

data class SetRepresentativeCommand(
    val userCode: UserCode,
    val petId: PetId,
)

data class SetRepresentativeResult(
    val pet: Pet,
    val breed: BreedSummary,
)
