package com.petcampus.knockdog.domain.breed.application.port.output

import com.petcampus.knockdog.domain.breed.domain.Breed

interface LoadBreedsPort {
    fun findAllByDisplayOrder(): List<Breed>

    fun search(query: String): List<Breed>
}
