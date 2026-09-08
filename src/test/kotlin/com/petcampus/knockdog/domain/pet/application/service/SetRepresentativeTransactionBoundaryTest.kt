package com.petcampus.knockdog.domain.pet.application.service

import com.petcampus.knockdog.domain.auth.adapter.outbound.persistence.UserJpaEntity
import com.petcampus.knockdog.domain.auth.adapter.outbound.persistence.UserJpaRepository
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.breed.adapter.outbound.persistence.BreedJpaEntity
import com.petcampus.knockdog.domain.breed.adapter.outbound.persistence.BreedJpaRepository
import com.petcampus.knockdog.domain.pet.adapter.outbound.persistence.PetJpaRepository
import com.petcampus.knockdog.domain.pet.adapter.outbound.persistence.PetPersistenceAdapter
import com.petcampus.knockdog.domain.pet.application.port.input.SetRepresentativeCommand
import com.petcampus.knockdog.domain.pet.application.port.input.SetRepresentativeUseCase
import com.petcampus.knockdog.domain.pet.domain.Gender
import com.petcampus.knockdog.domain.pet.domain.Pet
import com.petcampus.knockdog.domain.pet.domain.Relationship
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Testcontainers
@SpringBootTest
@ActiveProfiles("testcontainers")
class SetRepresentativeTransactionBoundaryTest {
    @Autowired
    private lateinit var setRepresentativeUseCase: SetRepresentativeUseCase

    @Autowired
    private lateinit var petPersistenceAdapter: PetPersistenceAdapter

    @Autowired
    private lateinit var petJpaRepository: PetJpaRepository

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var breedJpaRepository: BreedJpaRepository

    @Test
    fun `대표견 지정 뒤 breed 조회가 실패하면 대표견 변경도 롤백된다`() {
        val user = userJpaRepository.save(UserJpaEntity(userCode = UserCode.generate().value))
        val userId = requireNotNull(user.id)

        val breed =
            breedJpaRepository.save(
                BreedJpaEntity(
                    displayOrder = (1..1_000_000).random(),
                    fciStandardNumber = null,
                    nameEn = "Transaction Boundary Test Breed",
                    nameKo = "트랜잭션 경계 테스트 견종",
                    alias = null,
                ),
            )
        val breedId = requireNotNull(breed.id)

        val representative = petPersistenceAdapter.registerWithinLimit(newPet(userId, breedId, "보리"))
        val target = petPersistenceAdapter.registerWithinLimit(newPet(userId, breedId, "콩이"))

        breedJpaRepository.deleteById(breedId)

        assertFailsWith<IllegalStateException> {
            setRepresentativeUseCase.setRepresentative(
                SetRepresentativeCommand(userCode = UserCode(user.userCode), petId = requireNotNull(target.id)),
            )
        }

        val reloadedRepresentative = petJpaRepository.findById(requireNotNull(representative.id).value).orElseThrow()
        val reloadedTarget = petJpaRepository.findById(requireNotNull(target.id).value).orElseThrow()
        assertEquals(userId, reloadedRepresentative.representativeUserId, "breed 조회 실패로 전체가 롤백돼 기존 대표견이 그대로 유지돼야 한다")
        assertEquals(null, reloadedTarget.representativeUserId, "롤백됐으니 target은 대표견으로 지정되지 않은 상태로 남아야 한다")
    }

    private fun newPet(
        userId: Long,
        breedId: Long,
        name: String,
    ): Pet =
        Pet.create(
            userId = userId,
            name = name,
            profileImage = null,
            relationship = Relationship.GUARDIAN,
            relationshipText = null,
            breedId = breedId,
            gender = Gender.MALE,
            birthYear = null,
            weight = 10.0,
            isNeutered = null,
            isRepresentative = false,
        )

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val mysql: MySQLContainer<*> = MySQLContainer("mysql:8.0")
    }
}
