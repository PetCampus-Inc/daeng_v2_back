package com.petcampus.knockdog.domain.pet.adapter.outbound.persistence

import com.petcampus.knockdog.domain.auth.adapter.outbound.persistence.UserJpaEntity
import com.petcampus.knockdog.domain.auth.adapter.outbound.persistence.UserJpaRepository
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.breed.adapter.outbound.persistence.BreedJpaEntity
import com.petcampus.knockdog.domain.breed.adapter.outbound.persistence.BreedJpaRepository
import com.petcampus.knockdog.domain.pet.domain.Gender
import com.petcampus.knockdog.domain.pet.domain.Pet
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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Testcontainers
@SpringBootTest
@ActiveProfiles("testcontainers")
class PetSetRepresentativeConcurrencyTest {
    @Autowired
    private lateinit var petPersistenceAdapter: PetPersistenceAdapter

    @Autowired
    private lateinit var petJpaRepository: PetJpaRepository

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var breedJpaRepository: BreedJpaRepository

    private var userId: Long = 0
    private var breedId: Long = 0

    @BeforeEach
    fun setUp() {
        val user = userJpaRepository.save(UserJpaEntity(userCode = UserCode.generate().value))
        userId = requireNotNull(user.id)

        val breed =
            breedJpaRepository.save(
                BreedJpaEntity(
                    displayOrder = (1..1_000_000).random(),
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
        val pets = List(5) { i -> petPersistenceAdapter.registerWithinLimit(newPet("pet-$i")) }

        val threadCount = pets.size
        val executor = Executors.newFixedThreadPool(threadCount)
        val readyLatch = CountDownLatch(threadCount)
        val startLatch = CountDownLatch(1)

        val futures: List<Future<*>> =
            pets.map { pet ->
                executor.submit {
                    readyLatch.countDown()
                    startLatch.await()
                    petPersistenceAdapter.setRepresentativeWithinLock(pet)
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

    private fun newPet(name: String): Pet =
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
