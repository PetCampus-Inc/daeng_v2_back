package com.petcampus.knockdog.domain.pet.application.port.output

interface ExistsBreedPort {
    fun existsById(breedId: Long): Boolean
}
