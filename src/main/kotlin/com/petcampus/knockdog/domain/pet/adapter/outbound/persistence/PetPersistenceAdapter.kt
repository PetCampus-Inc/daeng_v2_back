package com.petcampus.knockdog.domain.pet.adapter.outbound.persistence

import com.petcampus.knockdog.domain.auth.adapter.outbound.persistence.UserJpaEntity
import com.petcampus.knockdog.domain.breed.adapter.outbound.persistence.BreedJpaEntity
import com.petcampus.knockdog.domain.pet.application.port.output.LoadPetPort
import com.petcampus.knockdog.domain.pet.application.port.output.SavePetPort
import com.petcampus.knockdog.domain.pet.domain.Pet
import com.petcampus.knockdog.domain.pet.domain.PetId
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class PetPersistenceAdapter(
    private val petJpaRepository: PetJpaRepository,
    private val entityManager: EntityManager,
) : LoadPetPort,
    SavePetPort {
    override fun findById(id: PetId): Pet? = petJpaRepository.findByIdOrNull(id.value)?.toDomain()

    override fun findAllActiveByUserId(userId: Long): List<Pet> = petJpaRepository.findAllActiveByUserId(userId).map { it.toDomain() }

    override fun findAllActiveByUserIdForUpdate(userId: Long): List<Pet> {
        entityManager.flush()
        entityManager.clear()
        return petJpaRepository.findAllActiveByUserIdForUpdate(userId).map { it.toDomain() }
    }

    override fun save(pet: Pet): Pet {
        val userRef = entityManager.getReference(UserJpaEntity::class.java, pet.userId)
        val breedRef = entityManager.getReference(BreedJpaEntity::class.java, pet.breedId)
        return petJpaRepository.save(pet.toJpaEntity(userRef, breedRef)).toDomain()
    }

    override fun saveAndFlush(pet: Pet): Pet {
        val saved = save(pet)
        entityManager.flush()
        return saved
    }
}
