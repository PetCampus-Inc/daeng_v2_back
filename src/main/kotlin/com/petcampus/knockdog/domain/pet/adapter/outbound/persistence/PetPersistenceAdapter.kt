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
import org.springframework.transaction.annotation.Transactional

@Component
class PetPersistenceAdapter(
    private val petJpaRepository: PetJpaRepository,
    private val entityManager: EntityManager,
) : LoadPetPort,
    SavePetPort {
    override fun findById(id: PetId): Pet? = petJpaRepository.findByIdOrNull(id.value)?.toDomain()

    override fun findAllActiveByUserId(userId: Long): List<Pet> = petJpaRepository.findAllActiveByUserId(userId).map { it.toDomain() }

    @Transactional
    override fun registerWithinLimit(pet: Pet): Pet {
        val activePets = petJpaRepository.findAllActiveByUserIdForUpdate(pet.userId)
        check(activePets.size < Pet.MAX_ACTIVE_COUNT) { "최대 마릿수를 초과했습니다." }

        if (activePets.isEmpty()) pet.markAsRepresentative() else pet.clearRepresentative()

        return save(pet)
    }

    override fun save(pet: Pet): Pet {
        val userRef = entityManager.getReference(UserJpaEntity::class.java, pet.userId)
        val breedRef = entityManager.getReference(BreedJpaEntity::class.java, pet.breedId)
        return petJpaRepository.save(pet.toJpaEntity(userRef, breedRef)).toDomain()
    }
}
