package com.petcampus.knockdog.domain.pet.application.port.output

import com.petcampus.knockdog.domain.pet.domain.Pet

interface SavePetPort {
    fun save(pet: Pet): Pet

    fun saveAndFlush(pet: Pet): Pet
}
