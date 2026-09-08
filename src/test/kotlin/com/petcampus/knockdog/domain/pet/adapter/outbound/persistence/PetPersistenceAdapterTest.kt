package com.petcampus.knockdog.domain.pet.adapter.outbound.persistence

import com.petcampus.knockdog.domain.auth.adapter.outbound.persistence.UserPersistenceAdapter
import com.petcampus.knockdog.domain.pet.application.PetErrorCode
import com.petcampus.knockdog.domain.pet.domain.Gender
import com.petcampus.knockdog.domain.pet.domain.Pet
import com.petcampus.knockdog.domain.pet.domain.Relationship
import com.petcampus.knockdog.global.exception.BusinessException
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

    @Test
    fun `이미 대표견인 pet에 setRepresentativeWithinLock을 호출하면 그대로 유지된다`() {
        val representative = petPersistenceAdapter.registerWithinLimit(pet(userId = 1L))

        val result = petPersistenceAdapter.setRepresentativeWithinLock(representative)

        assertTrue(result.isRepresentative)
    }

    @Test
    fun `setRepresentativeWithinLock을 호출하면 기존 대표견을 해제하고 대상 pet을 대표견으로 설정한다`() {
        val previousRepresentative = petPersistenceAdapter.registerWithinLimit(pet(userId = 1L))
        val target = petPersistenceAdapter.registerWithinLimit(pet(userId = 1L))

        val result = petPersistenceAdapter.setRepresentativeWithinLock(target)

        assertTrue(result.isRepresentative)
        val reloadedPrevious = petPersistenceAdapter.findById(requireNotNull(previousRepresentative.id))
        assertEquals(false, reloadedPrevious?.isRepresentative)
    }

    @Test
    fun `대표견이 없는 상태에서도 setRepresentativeWithinLock으로 대표견을 설정할 수 있다`() {
        val onlyRepresentative = petPersistenceAdapter.registerWithinLimit(pet(userId = 1L))
        val target = petPersistenceAdapter.registerWithinLimit(pet(userId = 1L))
        onlyRepresentative.clearRepresentative()
        petPersistenceAdapter.save(onlyRepresentative)

        val result = petPersistenceAdapter.setRepresentativeWithinLock(target)

        assertTrue(result.isRepresentative)
    }

    @Test
    fun `setRepresentativeWithinLock 호출 전에 다른 필드가 동시에 변경돼도 그 변경을 덮어쓰지 않는다`() {
        val staleTarget = petPersistenceAdapter.registerWithinLimit(pet(userId = 1L, name = "호두"))
        val concurrentlyRenamed =
            Pet.reconstitute(
                id = requireNotNull(staleTarget.id),
                userId = staleTarget.userId,
                name = "산책왕",
                profileImage = staleTarget.profileImage,
                relationship = staleTarget.relationship,
                relationshipText = staleTarget.relationshipText,
                breedId = staleTarget.breedId,
                gender = staleTarget.gender,
                birthYear = staleTarget.birthYear,
                weight = staleTarget.weight,
                isNeutered = staleTarget.isNeutered,
                isRepresentative = staleTarget.isRepresentative,
                deletedAt = staleTarget.deletedAt,
            )
        petPersistenceAdapter.save(concurrentlyRenamed)

        val result = petPersistenceAdapter.setRepresentativeWithinLock(staleTarget)

        assertEquals("산책왕", result.name)
        val reloaded = petPersistenceAdapter.findById(requireNotNull(staleTarget.id))
        assertEquals("산책왕", reloaded?.name)
    }

    @Test
    fun `대표견을 삭제하면 남은 pet 중 이름순으로 다음 pet이 새 대표견이 된다`() {
        petPersistenceAdapter.registerWithinLimit(pet(userId = 1L, name = "가온"))
        val toBecomeRepresentative = petPersistenceAdapter.registerWithinLimit(pet(userId = 1L, name = "나비"))
        petPersistenceAdapter.registerWithinLimit(pet(userId = 1L, name = "다롱"))
        val target = petPersistenceAdapter.setRepresentativeWithinLock(toBecomeRepresentative)

        val promoted = petPersistenceAdapter.deleteAndPromoteWithinLock(target)

        assertEquals("가온", promoted?.name)
        assertTrue(requireNotNull(promoted).isRepresentative)
        val reloadedTarget = petJpaRepository.findById(requireNotNull(target.id).value).orElseThrow()
        assertTrue(reloadedTarget.deletedAt != null)
        assertEquals(null, reloadedTarget.representativeUserId)
    }

    @Test
    fun `대표견을 삭제했는데 남은 pet이 없으면 대표견 없음 상태가 된다`() {
        val onlyPet = petPersistenceAdapter.registerWithinLimit(pet(userId = 1L))

        val promoted = petPersistenceAdapter.deleteAndPromoteWithinLock(onlyPet)

        assertEquals(null, promoted)
        val reloaded = petJpaRepository.findById(requireNotNull(onlyPet.id).value).orElseThrow()
        assertTrue(reloaded.deletedAt != null)
        assertEquals(null, reloaded.representativeUserId)
    }

    @Test
    fun `대표견을 삭제한 뒤 다른 pet이 새 대표견으로 승격돼도 유니크 제약에 걸리지 않는다`() {
        val representative = petPersistenceAdapter.registerWithinLimit(pet(userId = 1L, name = "가온"))
        petPersistenceAdapter.registerWithinLimit(pet(userId = 1L, name = "나비"))

        petPersistenceAdapter.deleteAndPromoteWithinLock(representative)
        petJpaRepository.flush()

        val activePets = petJpaRepository.findAllActiveByUserId(1L)
        assertEquals(1, activePets.size)
        assertEquals("나비", activePets.single().name)
        assertTrue(activePets.single().representativeUserId != null)
    }

    @Test
    fun `대표견이 아닌 pet을 deleteAndPromoteWithinLock으로 삭제해도 기존 대표견은 그대로 유지된다`() {
        val representative = petPersistenceAdapter.registerWithinLimit(pet(userId = 1L, name = "가온"))
        val other = petPersistenceAdapter.registerWithinLimit(pet(userId = 1L, name = "나비"))

        val promoted = petPersistenceAdapter.deleteAndPromoteWithinLock(other)

        assertEquals(null, promoted)
        val reloadedRepresentative = petJpaRepository.findById(requireNotNull(representative.id).value).orElseThrow()
        assertTrue(reloadedRepresentative.representativeUserId != null)
    }

    @Test
    fun `잠금 재조회 시점에 이미 삭제된 pet이면 500이 아니라 NOT_FOUND를 던진다`() {
        val target = petPersistenceAdapter.registerWithinLimit(pet(userId = 1L, name = "가온"))
        target.delete()
        petPersistenceAdapter.save(target)

        val exception = assertFailsWith<BusinessException> { petPersistenceAdapter.deleteAndPromoteWithinLock(target) }

        assertEquals(PetErrorCode.NOT_FOUND, exception.errorCode)
    }

    private fun pet(
        userId: Long,
        isRepresentative: Boolean = false,
        name: String = "호두",
    ) = Pet.create(
        userId = userId,
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
