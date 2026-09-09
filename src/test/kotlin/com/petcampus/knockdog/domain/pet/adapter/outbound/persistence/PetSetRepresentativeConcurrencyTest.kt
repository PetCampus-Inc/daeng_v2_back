package com.petcampus.knockdog.domain.pet.adapter.outbound.persistence

import com.petcampus.knockdog.domain.auth.adapter.outbound.persistence.UserJpaEntity
import com.petcampus.knockdog.domain.auth.adapter.outbound.persistence.UserJpaRepository
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.breed.adapter.outbound.persistence.BreedJpaEntity
import com.petcampus.knockdog.domain.breed.adapter.outbound.persistence.BreedJpaRepository
import com.petcampus.knockdog.domain.pet.application.port.input.CreatePetCommand
import com.petcampus.knockdog.domain.pet.application.port.input.CreatePetUseCase
import com.petcampus.knockdog.domain.pet.application.port.input.SetRepresentativeCommand
import com.petcampus.knockdog.domain.pet.application.port.input.SetRepresentativeUseCase
import com.petcampus.knockdog.domain.pet.domain.Gender
import com.petcampus.knockdog.domain.pet.domain.Relationship
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Testcontainers
@SpringBootTest
@ActiveProfiles("testcontainers")
class PetSetRepresentativeConcurrencyTest {
    @Autowired
    private lateinit var createPetUseCase: CreatePetUseCase

    @Autowired
    private lateinit var setRepresentativeUseCase: SetRepresentativeUseCase

    @Autowired
    private lateinit var petJpaRepository: PetJpaRepository

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var breedJpaRepository: BreedJpaRepository

    private var userId: Long = 0
    private var userCode: UserCode = UserCode.generate()
    private var breedId: Long = 0

    @BeforeEach
    fun setUp() {
        val code = UserCode.generate()
        val user = userJpaRepository.save(UserJpaEntity(userCode = code.value))
        userId = requireNotNull(user.id)
        userCode = code

        val breed =
            breedJpaRepository.save(
                BreedJpaEntity(
                    displayOrder = nextDisplayOrder(),
                    fciStandardNumber = null,
                    nameEn = "Concurrency Test Breed",
                    nameKo = "동시성 테스트 견종",
                    alias = null,
                ),
            )
        breedId = requireNotNull(breed.id)
    }

    @Test
    fun `동일 사용자의 대표견 설정 요청 5건을 동시에 실행해도 대표견은 1건만 남는다`() {
        val petIds = List(5) { i -> createPetUseCase.create(newCommand("pet-$i")).pet.id!! }

        val executor = Executors.newFixedThreadPool(petIds.size)
        val readyLatch = CountDownLatch(petIds.size)
        val startLatch = CountDownLatch(1)

        val futures: List<Future<*>> =
            petIds.map { petId ->
                executor.submit {
                    readyLatch.countDown()
                    startLatch.await()
                    setRepresentativeUseCase.setRepresentative(SetRepresentativeCommand(userCode, petId))
                }
            }

        readyLatch.await(10, TimeUnit.SECONDS)
        startLatch.countDown()
        val failures = futures.mapNotNull { runCatching { it.get(30, TimeUnit.SECONDS) }.exceptionOrNull() }
        executor.shutdown()

        assertTrue(failures.isEmpty(), "동시 요청 중 실패 발생: $failures")
        val savedPets = petJpaRepository.findAllActiveByUserId(userId)
        assertEquals(5, savedPets.size)
        assertEquals(1, savedPets.count { it.representativeUserId != null })
    }

    private fun newCommand(name: String) =
        CreatePetCommand(
            userCode = userCode,
            name = name,
            profileImage = null,
            relationship = Relationship.GUARDIAN,
            relationshipText = null,
            breedId = breedId,
            gender = Gender.MALE,
            birthYear = null,
            weight = 10.0,
            isNeutered = null,
        )

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val mysql: MySQLContainer<*> = MySQLContainer("mysql:8.0")

        private val displayOrderSequence = AtomicInteger(1_000_000)

        private fun nextDisplayOrder(): Int = displayOrderSequence.incrementAndGet()
    }
}
