package com.petcampus.knockdog.domain.pet.application.port.input

import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.pet.application.port.output.BreedSummary
import com.petcampus.knockdog.domain.pet.domain.Gender
import com.petcampus.knockdog.domain.pet.domain.Pet
import com.petcampus.knockdog.domain.pet.domain.PetId
import com.petcampus.knockdog.domain.pet.domain.Relationship
import org.openapitools.jackson.nullable.JsonNullable

interface UpdatePetUseCase {
    fun update(command: UpdatePetCommand): UpdatePetResult
}

data class UpdatePetCommand(
    val userCode: UserCode,
    val petId: PetId,
    val name: JsonNullable<String> = JsonNullable.undefined(),
    val profileImage: JsonNullable<String?> = JsonNullable.undefined(),
    val relationship: JsonNullable<Relationship> = JsonNullable.undefined(),
    val relationshipText: JsonNullable<String?> = JsonNullable.undefined(),
    val breedId: JsonNullable<Long> = JsonNullable.undefined(),
    val gender: JsonNullable<Gender> = JsonNullable.undefined(),
    val birthYear: JsonNullable<Int?> = JsonNullable.undefined(),
    val weight: JsonNullable<Double> = JsonNullable.undefined(),
    val isNeutered: JsonNullable<Boolean?> = JsonNullable.undefined(),
)

data class UpdatePetResult(
    val pet: Pet,
    val breed: BreedSummary,
)
