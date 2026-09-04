package com.petcampus.knockdog.domain.pet.adapter.outbound.persistence

import com.petcampus.knockdog.domain.pet.domain.Gender
import com.petcampus.knockdog.domain.pet.domain.Pet
import com.petcampus.knockdog.domain.pet.domain.Relationship
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@DataJpaTest
@Import(PetPersistenceAdapter::class)
class PetPersistenceAdapterTest(
    @Autowired private val petPersistenceAdapter: PetPersistenceAdapter,
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
    fun `동일 사용자의 두 번째 대표견 저장은 유니크 제약 위반으로 실패한다`() {
        petPersistenceAdapter.save(pet(userId = 1L, isRepresentative = true))

        assertFailsWith<DataIntegrityViolationException> {
            petPersistenceAdapter.save(pet(userId = 1L, isRepresentative = true))
        }
    }

    @Test
    fun `대표견을 삭제한 뒤 새 pet을 등록해도 유니크 제약에 걸리지 않는다`() {
        val representative = petPersistenceAdapter.registerWithinLimit(pet(userId = 1L))
        representative.delete()
        petPersistenceAdapter.save(representative)

        val result = petPersistenceAdapter.registerWithinLimit(pet(userId = 1L))

        assertTrue(result.isRepresentative)
    }

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
        weight = null,
        isNeutered = null,
        isRepresentative = isRepresentative,
    )
}
