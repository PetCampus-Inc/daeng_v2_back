package com.petcampus.knockdog.domain.pet.adapter.outbound.persistence

import com.petcampus.knockdog.domain.auth.adapter.outbound.persistence.UserJpaEntity
import com.petcampus.knockdog.domain.breed.adapter.outbound.persistence.BreedJpaEntity
import com.petcampus.knockdog.domain.pet.domain.Pet
import com.petcampus.knockdog.domain.pet.domain.PetId

fun Pet.toJpaEntity(
    userRef: UserJpaEntity,
    breedRef: BreedJpaEntity,
): PetJpaEntity =
    PetJpaEntity(
        id = id?.value,
        user = userRef,
        name = name,
        profileImage = profileImage,
        relationship = relationship,
        relationshipText = relationshipText,
        breed = breedRef,
        gender = gender,
        birthYear = birthYear,
        weight = weight,
        isNeutered = isNeutered,
        representativeUserId = if (isRepresentative) userId else null,
        deletedAt = deletedAt,
    )

fun PetJpaEntity.toDomain(): Pet =
    Pet.reconstitute(
        id = PetId(requireNotNull(id) { "저장되지 않은 PetJpaEntity입니다." }),
        userId = requireNotNull(user.id) { "저장되지 않은 UserJpaEntity 참조입니다." },
        name = name,
        profileImage = profileImage,
        relationship = relationship,
        relationshipText = relationshipText,
        breedId = requireNotNull(breed.id) { "저장되지 않은 BreedJpaEntity 참조입니다." },
        gender = gender,
        birthYear = birthYear,
        weight = weight,
        isNeutered = isNeutered,
        isRepresentative = representativeUserId != null,
        deletedAt = deletedAt,
    )
