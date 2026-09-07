package com.petcampus.knockdog.domain.pet.application.port.output

import com.petcampus.knockdog.domain.pet.domain.Pet
import com.petcampus.knockdog.domain.pet.domain.PetId

interface LoadPetPort {
    fun findById(id: PetId): Pet?

    fun findAllActiveByUserId(userId: Long): List<Pet>
}
