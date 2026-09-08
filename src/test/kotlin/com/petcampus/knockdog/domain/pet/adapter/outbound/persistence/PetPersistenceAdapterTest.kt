package com.petcampus.knockdog.domain.pet.adapter.outbound.persistence

import com.petcampus.knockdog.domain.auth.adapter.outbound.persistence.UserPersistenceAdapter
import com.petcampus.knockdog.domain.pet.domain.Gender
import com.petcampus.knockdog.domain.pet.domain.Pet
import com.petcampus.knockdog.domain.pet.domain.Relationship
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.OptimisticLockingFailureException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@DataJpaTest
@Import(PetPersistenceAdapter::class, UserPersistenceAdapter::class)
class PetPersistenceAdapterTest(
    @Autowired private val petPersistenceAdapter: PetPersistenceAdapter,
    @Autowired private val petJpaRepository: PetJpaRepository,
    @Autowired private val entityManager: EntityManager,
) {
    @Test
    fun `첫 등록은 자동으로 대표견이 된다`() {
        val result = petPersistenceAdapter.registerWithinLimit(pet(userId = 1L))

        assertTrue(result.isRepresentative)
    }

    @Test
    fun `두 번째 등록부터는 대표견으로 지정되지 않는다`() {
        petPersistenceAdapter.registerWithinLimit(pet(userId = 1L))

        val result = petPersistenceAdapter.registerWithinLimit(pet(userId = 1L))

        assertEquals(false, result.isRepresentative)
    }

    @Test
    fun `활성 pet이 5마리면 등록을 거부한다`() {
        repeat(5) { petPersistenceAdapter.registerWithinLimit(pet(userId = 1L)) }

        assertFailsWith<IllegalStateException> { petPersistenceAdapter.registerWithinLimit(pet(userId = 1L)) }
    }

    @Test
    fun `다른 사용자의 활성 pet 수는 최대 마릿수 판단에 영향을 주지 않는다`() {
        repeat(5) { petPersistenceAdapter.registerWithinLimit(pet(userId = 1L)) }

        val result = petPersistenceAdapter.registerWithinLimit(pet(userId = 2L))

        assertTrue(result.isRepresentative)
    }

    @Test
    fun `대표견을 삭제한 뒤 새 pet을 등록해도 유니크 제약에 걸리지 않는다`() {
        val representative = petPersistenceAdapter.registerWithinLimit(pet(userId = 1L))
        representative.delete()
        petPersistenceAdapter.save(representative)

        val result = petPersistenceAdapter.registerWithinLimit(pet(userId = 1L))

        assertTrue(result.isRepresentative)
    }

    @Test
    fun `기존 대표견을 해제하고 다른 pet을 대표견으로 바꿀 수 있다`() {
        val previousRepresentative = petPersistenceAdapter.registerWithinLimit(pet(userId = 1L))
        val newRepresentative = petPersistenceAdapter.registerWithinLimit(pet(userId = 1L))

        previousRepresentative.clearRepresentative()
        petPersistenceAdapter.save(previousRepresentative)
        newRepresentative.markAsRepresentative()
        val result = petPersistenceAdapter.save(newRepresentative)

        assertTrue(result.isRepresentative)
    }

    @Test
    fun `읽은 시점 이후 버전이 바뀐 pet을 저장하면 낙관적 락 충돌이 발생한다`() {
        val registered = petPersistenceAdapter.registerWithinLimit(pet(userId = 1L))
        val petId = requireNotNull(registered.id)
        entityManager.clear()

        val stale = requireNotNull(petPersistenceAdapter.findById(petId))
        entityManager.clear()

        val fresh = requireNotNull(petPersistenceAdapter.findById(petId))
        renameTo(fresh, "보리")
        petPersistenceAdapter.save(fresh)
        petJpaRepository.flush()
        entityManager.clear()

        renameTo(stale, "메리")
        assertFailsWith<OptimisticLockingFailureException> {
            petPersistenceAdapter.save(stale)
            petJpaRepository.flush()
        }
    }

    private fun renameTo(
        pet: Pet,
        name: String,
    ) = pet.update(
        name = name,
        profileImage = pet.profileImage,
        relationship = pet.relationship,
        relationshipText = pet.relationshipText,
        breedId = pet.breedId,
        gender = pet.gender,
        birthYear = pet.birthYear,
        weight = pet.weight,
        isNeutered = pet.isNeutered,
    )

    private fun pet(
        userId: Long,
        isRepresentative: Boolean = false,
    ) = Pet.create(
        userId = userId,
        name = "호두",
        profileImage = null,
        relationship = Relationship.GUARDIAN,
        relationshipText = null,
        breedId = 1L,
        gender = Gender.MALE,
        birthYear = 2020,
        weight = 10.0,
        isNeutered = null,
        isRepresentative = isRepresentative,
    )
}
