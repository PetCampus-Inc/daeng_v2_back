package com.petcampus.knockdog.domain.pet.adapter.outbound.persistence

import com.petcampus.knockdog.domain.breed.application.port.output.LoadBreedsPort
import com.petcampus.knockdog.domain.pet.application.port.output.BreedSummary
import com.petcampus.knockdog.domain.pet.application.port.output.LoadBreedPort
import org.springframework.stereotype.Component

@Component
class BreedSummaryAdapter(
    private val loadBreedsPort: LoadBreedsPort,
) : LoadBreedPort {
    override fun findById(breedId: Long): BreedSummary? =
        loadBreedsPort.findById(breedId)?.let { BreedSummary(id = it.id, nameKo = it.nameKo, alias = it.alias) }
}
