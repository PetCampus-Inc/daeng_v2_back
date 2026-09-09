package com.petcampus.knockdog.domain.pet.adapter.outbound.persistence

import com.petcampus.knockdog.domain.auth.adapter.outbound.persistence.UserJpaEntity
import com.petcampus.knockdog.domain.auth.adapter.outbound.persistence.UserJpaRepository
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.breed.adapter.outbound.persistence.BreedJpaEntity
import com.petcampus.knockdog.domain.breed.adapter.outbound.persistence.BreedJpaRepository
import com.petcampus.knockdog.domain.pet.application.port.input.CreatePetCommand
import com.petcampus.knockdog.domain.pet.application.port.input.CreatePetUseCase
import com.petcampus.knockdog.domain.pet.application.port.input.DeletePetCommand
import com.petcampus.knockdog.domain.pet.application.port.input.DeletePetUseCase
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
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Testcontainers
@SpringBootTest
@ActiveProfiles("testcontainers")
class PetDeleteAndPromoteConcurrencyTest {
    @Autowired
    private lateinit var createPetUseCase: CreatePetUseCase

    @Autowired
    private lateinit var deletePetUseCase: DeletePetUseCase

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
                    displayOrder = (1..1_000_000).random(),
                    fciStandardNumber = null,
                    nameEn = "Delete Concurrency Test Breed",
                    nameKo = "삭제 동시성 테스트 견종",
                    alias = null,
                ),
            )
        breedId = requireNotNull(breed.id)
    }

    @Test
    fun `대표견 삭제와 다른 pet의 대표견 설정이 동시에 들어와도 대표견은 1건만 남는다`() {
        val representative = createPetUseCase.create(newCommand("가온")).pet
        val other = createPetUseCase.create(newCommand("나비")).pet
        createPetUseCase.create(newCommand("다롱"))

        val executor = Executors.newFixedThreadPool(2)
        val readyLatch = CountDownLatch(2)
        val startLatch = CountDownLatch(1)

        val deleteFuture =
            executor.submit {
                readyLatch.countDown()
                startLatch.await()
                deletePetUseCase.delete(DeletePetCommand(userCode, representative.id!!))
            }
        val setRepresentativeFuture =
            executor.submit {
                readyLatch.countDown()
                startLatch.await()
                setRepresentativeUseCase.setRepresentative(SetRepresentativeCommand(userCode, other.id!!))
            }

        readyLatch.await(10, TimeUnit.SECONDS)
        startLatch.countDown()
        val failures =
            listOf(deleteFuture, setRepresentativeFuture)
                .mapNotNull { runCatching { it.get(30, TimeUnit.SECONDS) }.exceptionOrNull() }
        executor.shutdown()

        assertTrue(failures.isEmpty(), "동시 요청 중 실패 발생: $failures")
        val activePets = petJpaRepository.findAllActiveByUserId(userId)
        assertEquals(2, activePets.size)
        assertEquals(1, activePets.count { it.representativeUserId != null })
        val reloadedRepresentative = petJpaRepository.findById(requireNotNull(representative.id).value).orElseThrow()
        assertTrue(reloadedRepresentative.deletedAt != null)
        assertEquals(null, reloadedRepresentative.representativeUserId)
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
    }
}
