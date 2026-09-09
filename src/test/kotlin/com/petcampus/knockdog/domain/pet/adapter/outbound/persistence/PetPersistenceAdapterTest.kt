package com.petcampus.knockdog.domain.pet.adapter.outbound.persistence

import com.petcampus.knockdog.domain.auth.adapter.outbound.persistence.UserPersistenceAdapter
import com.petcampus.knockdog.domain.auth.domain.UserId
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

@DataJpaTest
@Import(PetPersistenceAdapter::class, UserPersistenceAdapter::class)
class PetPersistenceAdapterTest(
    @Autowired private val petPersistenceAdapter: PetPersistenceAdapter,
    @Autowired private val petJpaRepository: PetJpaRepository,
    @Autowired private val entityManager: EntityManager,
) {
    @Test
    fun `save한 pet을 findById로 다시 읽을 수 있다`() {
        val saved = petPersistenceAdapter.save(pet(userId = 1L, name = "호두"))

        val found = petPersistenceAdapter.findById(requireNotNull(saved.id))

        assertEquals("호두", found?.name)
    }

    @Test
    fun `saveAndFlush도 save와 동일하게 pet을 저장한다`() {
        val saved = petPersistenceAdapter.saveAndFlush(pet(userId = 1L, name = "호두"))

        val found = petPersistenceAdapter.findById(requireNotNull(saved.id))

        assertEquals("호두", found?.name)
    }

    @Test
    fun `findAllActiveByUserId는 삭제된 pet을 제외한다`() {
        petPersistenceAdapter.save(pet(userId = 1L, name = "가온"))
        val deleted = petPersistenceAdapter.save(pet(userId = 1L, name = "나비"))
        deleted.delete()
        petPersistenceAdapter.save(deleted)

        val result = petPersistenceAdapter.findAllActiveByUserId(UserId(1L))

        assertEquals(listOf("가온"), result.map { it.name })
    }

    @Test
    fun `findAllActiveByUserIdForUpdate도 삭제된 pet을 제외한 활성 pet만 반환한다`() {
        petPersistenceAdapter.save(pet(userId = 1L, name = "가온"))
        val deleted = petPersistenceAdapter.save(pet(userId = 1L, name = "나비"))
        deleted.delete()
        petPersistenceAdapter.save(deleted)

        val result = petPersistenceAdapter.findAllActiveByUserIdForUpdate(UserId(1L))

        assertEquals(listOf("가온"), result.map { it.name })
    }

    @Test
    fun `다른 사용자의 pet은 findAllActiveByUserId 결과에 섞이지 않는다`() {
        petPersistenceAdapter.save(pet(userId = 1L, name = "가온"))
        petPersistenceAdapter.save(pet(userId = 2L, name = "나비"))

        val result = petPersistenceAdapter.findAllActiveByUserId(UserId(1L))

        assertEquals(listOf("가온"), result.map { it.name })
    }

    @Test
    fun `읽은 시점 이후 버전이 바뀐 pet을 저장하면 낙관적 락 충돌이 발생한다`() {
        val registered = petPersistenceAdapter.save(pet(userId = 1L))
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
        name: String = "호두",
    ) = Pet.create(
        userId = UserId(userId),
        name = name,
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
