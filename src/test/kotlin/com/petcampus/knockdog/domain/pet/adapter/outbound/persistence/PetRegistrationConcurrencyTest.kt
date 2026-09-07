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
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

@Testcontainers
@SpringBootTest
@ActiveProfiles("testcontainers")
class PetRegistrationConcurrencyTest {
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
    fun `활성 pet 0건 상태에서 동일 사용자의 최초 등록 6건을 동시에 실행하면 5건만 성공하고 대표견은 1건이다`() {
        val threadCount = 6
        val executor = Executors.newFixedThreadPool(threadCount)
        val readyLatch = CountDownLatch(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val limitExceededCount = AtomicInteger(0)

        repeat(threadCount) { i ->
            executor.submit {
                readyLatch.countDown()
                startLatch.await()
                try {
                    petPersistenceAdapter.registerWithinLimit(newPet("concurrency-pet-$i"))
                    successCount.incrementAndGet()
                } catch (e: IllegalStateException) {
                    limitExceededCount.incrementAndGet()
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        readyLatch.await(10, TimeUnit.SECONDS)
        startLatch.countDown()
        doneLatch.await(30, TimeUnit.SECONDS)
        executor.shutdown()

        assertEquals(5, successCount.get())
        assertEquals(1, limitExceededCount.get())

        val savedPets = petJpaRepository.findAllActiveByUserId(userId)
        assertEquals(5, savedPets.size)
        assertEquals(1, savedPets.count { it.representativeUserId != null })
    }

    @Test
    fun `기존 4마리가 등록된 상태에서 신규 등록 3건을 동시에 실행하면 1건만 성공한다`() {
        repeat(4) { i -> petPersistenceAdapter.registerWithinLimit(newPet("existing-pet-$i")) }

        val threadCount = 3
        val executor = Executors.newFixedThreadPool(threadCount)
        val readyLatch = CountDownLatch(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val limitExceededCount = AtomicInteger(0)

        repeat(threadCount) { i ->
            executor.submit {
                readyLatch.countDown()
                startLatch.await()
                try {
                    petPersistenceAdapter.registerWithinLimit(newPet("race-pet-$i"))
                    successCount.incrementAndGet()
                } catch (e: IllegalStateException) {
                    limitExceededCount.incrementAndGet()
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        readyLatch.await(10, TimeUnit.SECONDS)
        startLatch.countDown()
        doneLatch.await(30, TimeUnit.SECONDS)
        executor.shutdown()

        assertEquals(1, successCount.get())
        assertEquals(2, limitExceededCount.get())

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
