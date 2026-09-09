package com.petcampus.knockdog.domain.pet.application.service

import com.petcampus.knockdog.domain.auth.application.AuthErrorCode
import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.LockUserPort
import com.petcampus.knockdog.domain.auth.domain.AddressType
import com.petcampus.knockdog.domain.auth.domain.User
import com.petcampus.knockdog.domain.auth.domain.UserAddress
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.auth.domain.UserId
import com.petcampus.knockdog.domain.pet.application.PetErrorCode
import com.petcampus.knockdog.domain.pet.application.port.input.CreatePetCommand
import com.petcampus.knockdog.domain.pet.application.port.output.BreedSummary
import com.petcampus.knockdog.domain.pet.application.port.output.LoadBreedPort
import com.petcampus.knockdog.domain.pet.application.port.output.LoadPetPort
import com.petcampus.knockdog.domain.pet.application.port.output.SavePetPort
import com.petcampus.knockdog.domain.pet.domain.Gender
import com.petcampus.knockdog.domain.pet.domain.Pet
import com.petcampus.knockdog.domain.pet.domain.PetId
import com.petcampus.knockdog.domain.pet.domain.Relationship
import com.petcampus.knockdog.global.exception.BusinessException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CreatePetServiceTest {
    @Test
    fun `정상 생성 시 breed 정보를 포함한 결과를 반환한다`() {
        val service = service(activePets = emptyList())

        val result = service.create(command(breedId = 4L))

        assertEquals("골든 리트리버", result.breed.nameKo)
        assertEquals(1L, result.pet.userId)
    }

    @Test
    fun `첫 등록은 자동으로 대표견이 된다`() {
        val service = service(activePets = emptyList())

        val result = service.create(command(breedId = 4L))

        assertTrue(result.pet.isRepresentative)
    }

    @Test
    fun `두 번째 등록부터는 대표견으로 지정되지 않는다`() {
        val service = service(activePets = listOf(existingPet()))

        val result = service.create(command(breedId = 4L))

        assertFalse(result.pet.isRepresentative)
    }

    @Test
    fun `존재하지 않는 사용자면 NOT_FOUND_USER를 던진다`() {
        val service = service(activePets = emptyList(), userId = null)

        val exception = assertFailsWith<BusinessException> { service.create(command(breedId = 4L)) }

        assertEquals(AuthErrorCode.NOT_FOUND_USER, exception.errorCode)
    }

    @Test
    fun `존재하지 않는 breedId면 NOT_FOUND_BREED를 던진다`() {
        val service = service(activePets = emptyList(), breed = null)

        val exception = assertFailsWith<BusinessException> { service.create(command(breedId = 999L)) }

        assertEquals(PetErrorCode.NOT_FOUND_BREED, exception.errorCode)
    }

    @Test
    fun `최대 마릿수 초과 시 LIMIT_EXCEEDED를 던진다`() {
        val service = service(activePets = List(Pet.MAX_ACTIVE_COUNT) { existingPet() })

        val exception = assertFailsWith<BusinessException> { service.create(command(breedId = 4L)) }

        assertEquals(PetErrorCode.LIMIT_EXCEEDED, exception.errorCode)
    }

    private fun service(
        activePets: List<Pet>,
        userId: Long? = 1L,
        breed: BreedSummary? = BreedSummary(4L, "골든 리트리버", null),
    ) = CreatePetService(
        loadUserPort = FakeLoadUserPort(userId),
        loadBreedPort = FakeLoadBreedPort(breed),
        savePetPort = RecordingSavePetPort(),
        petLockOperations = PetLockOperations(NoopLockUserPort(), FakeLoadPetPort(activePets)),
    )

    private fun command(breedId: Long) =
        CreatePetCommand(
            userCode = UserCode("ABCD1234"),
            name = "호두",
            profileImage = null,
            relationship = Relationship.GUARDIAN,
            relationshipText = null,
            breedId = breedId,
            gender = Gender.MALE,
            birthYear = 2020,
            weight = 10.0,
            isNeutered = null,
        )

    private fun existingPet() =
        Pet.reconstitute(
            id = PetId(1L),
            userId = 1L,
            name = "보리",
            profileImage = null,
            relationship = Relationship.GUARDIAN,
            relationshipText = null,
            breedId = 4L,
            gender = Gender.MALE,
            birthYear = 2020,
            weight = 10.0,
            isNeutered = null,
            isRepresentative = true,
            deletedAt = null,
        )

    private class FakeLoadUserPort(
        private val userId: Long?,
    ) : LoadUserPort {
        override fun findById(id: UserId): User? = null

        override fun findByCode(code: UserCode): User? =
            userId?.let {
                User.reconstitute(
                    id = UserId(it),
                    code = code,
                    nickname = null,
                    profileImage = null,
                    infoReceiveEmail = null,
                    gender = null,
                    phoneNumber = null,
                    emergencyPhoneNumber = null,
                    addresses = listOf(UserAddress.create(AddressType.HOME, null, "서울", null, 0.0, 0.0)),
                    deletedAt = null,
                )
            }
    }

    private class FakeLoadBreedPort(
        private val breed: BreedSummary?,
    ) : LoadBreedPort {
        override fun findById(breedId: Long): BreedSummary? = breed
    }

    private class FakeLoadPetPort(
        private val activePets: List<Pet>,
    ) : LoadPetPort {
        override fun findById(id: PetId): Pet? = activePets.find { it.id == id }

        override fun findAllActiveByUserId(userId: Long): List<Pet> = activePets

        override fun findAllActiveByUserIdForUpdate(userId: Long): List<Pet> = activePets
    }

    private class NoopLockUserPort : LockUserPort {
        override fun lockById(userId: Long) = Unit
    }

    private class RecordingSavePetPort : SavePetPort {
        override fun save(pet: Pet): Pet = pet

        override fun saveAndFlush(pet: Pet): Pet = pet
    }
}
