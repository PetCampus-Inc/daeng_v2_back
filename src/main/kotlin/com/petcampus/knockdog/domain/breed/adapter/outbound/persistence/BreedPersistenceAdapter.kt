package com.petcampus.knockdog.domain.breed.adapter.outbound.persistence

import com.petcampus.knockdog.domain.breed.application.port.output.LoadBreedsPort
import com.petcampus.knockdog.domain.breed.domain.Breed
import org.springframework.stereotype.Component

@Component
class BreedPersistenceAdapter(
    private val breedJpaRepository: BreedJpaRepository,
) : LoadBreedsPort {
    override fun findAllByDisplayOrder(): List<Breed> = breedJpaRepository.findAllByOrderByDisplayOrderAsc().map(BreedJpaEntity::toDomain)

    override fun search(query: String): List<Breed> = breedJpaRepository.search(query).map(BreedJpaEntity::toDomain)
}

private fun BreedJpaEntity.toDomain(): Breed =
    Breed(
        id = requireNotNull(id),
        displayOrder = displayOrder,
        fciStandardNumber = fciStandardNumber,
        nameEn = nameEn,
        nameKo = nameKo,
        alias = alias,
    )
