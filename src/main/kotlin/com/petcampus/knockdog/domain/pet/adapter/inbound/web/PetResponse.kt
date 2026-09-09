package com.petcampus.knockdog.domain.pet.adapter.inbound.web

import com.petcampus.knockdog.domain.pet.application.port.output.BreedSummary
import com.petcampus.knockdog.domain.pet.domain.Gender
import com.petcampus.knockdog.domain.pet.domain.Pet
import com.petcampus.knockdog.domain.pet.domain.Relationship

data class PetResponse(
    val id: Long,
    val name: String,
    val profileImage: String?,
    val relationship: Relationship,
    val relationshipText: String?,
    val breedId: Long,
    val breedNameKo: String,
    val breedAlias: String?,
    val gender: Gender,
    val birthYear: Int?,
    val weight: Double,
    val isNeutered: Boolean?,
    val isRepresentative: Boolean,
) {
    companion object {
        fun of(
            pet: Pet,
            breed: BreedSummary,
        ) = PetResponse(
            id = requireNotNull(pet.id) { "저장되지 않은 Pet입니다." }.value,
            name = pet.name,
            profileImage = pet.profileImage,
            relationship = pet.relationship,
            relationshipText = pet.relationshipText,
            breedId = pet.breedId,
            breedNameKo = breed.nameKo,
            breedAlias = breed.alias,
            gender = pet.gender,
            birthYear = pet.birthYear,
            weight = pet.weight,
            isNeutered = pet.isNeutered,
            isRepresentative = pet.isRepresentative,
        )
    }
}
