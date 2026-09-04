package com.petcampus.knockdog.domain.pet.adapter.outbound.persistence

import com.petcampus.knockdog.domain.breed.application.port.output.LoadBreedsPort
import com.petcampus.knockdog.domain.pet.application.port.output.ExistsBreedPort
import org.springframework.stereotype.Component

@Component
class BreedExistenceAdapter(
    private val loadBreedsPort: LoadBreedsPort,
) : ExistsBreedPort {
    override fun existsById(breedId: Long): Boolean = loadBreedsPort.existsById(breedId)
}
