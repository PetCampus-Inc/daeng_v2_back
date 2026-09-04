package com.petcampus.knockdog.domain.breed.application.port.input

import com.petcampus.knockdog.domain.breed.domain.Breed

interface GetBreedsUseCase {
    fun getBreeds(query: String?): List<Breed>
}
