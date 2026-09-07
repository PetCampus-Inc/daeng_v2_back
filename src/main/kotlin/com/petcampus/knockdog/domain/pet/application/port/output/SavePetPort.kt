package com.petcampus.knockdog.domain.pet.application.port.output

import com.petcampus.knockdog.domain.pet.domain.Pet

interface SavePetPort {
    fun registerWithinLimit(pet: Pet): Pet

    fun save(pet: Pet): Pet
}
