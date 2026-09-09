package com.petcampus.knockdog.domain.pet.application.service

import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserPort
import com.petcampus.knockdog.domain.auth.application.service.RequireUserId
import com.petcampus.knockdog.domain.auth.domain.AddressType
import com.petcampus.knockdog.domain.auth.domain.User
import com.petcampus.knockdog.domain.auth.domain.UserAddress
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.auth.domain.UserId
import com.petcampus.knockdog.domain.pet.application.port.input.GetPetCommand
import com.petcampus.knockdog.domain.pet.application.port.output.BreedSummary
import com.petcampus.knockdog.domain.pet.application.port.output.LoadBreedPort
import com.petcampus.knockdog.domain.pet.application.port.output.LoadPetPort
import com.petcampus.knockdog.domain.pet.domain.Gender
import com.petcampus.knockdog.domain.pet.domain.Pet
import com.petcampus.knockdog.domain.pet.domain.PetId
import com.petcampus.knockdog.domain.pet.domain.Relationship
import com.petcampus.knockdog.global.exception.BusinessException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetPetServiceTest {
    @Test
    fun `본인 소유 pet을 breed 정보와 함께 반환한다`() {
        val pet = pet()
        val service = service(pet = pet)

        val result = service.getPet(command(petId = pet.id!!))

        assertEquals("호두", result.pet.name)
        assertEquals("골든 리트리버", result.breed.nameKo)
    }

    @Test
    fun `존재하지 않는 pet이면 NOT_FOUND를 던진다`() {
        val service = service(pet = null)

        assertFailsWith<BusinessException> { service.getPet(command(petId = PetId(1L))) }
    }

    @Test
    fun `삭제된 pet이면 NOT_FOUND를 던진다`() {
        val pet = pet()
        pet.delete()
        val service = service(pet = pet)

        assertFailsWith<BusinessException> { service.getPet(command(petId = pet.id!!)) }
    }

    @Test
    fun `다른 사용자의 pet이면 NOT_AUTHORIZED를 던진다`() {
        val pet = pet(userId = 999L)
        val service = service(pet = pet)

        assertFailsWith<BusinessException> { service.getPet(command(petId = pet.id!!)) }
    }

    @Test
    fun `참조하는 breed가 없으면 500으로 이어지는 예외를 던진다`() {
        val pet = pet()
        val service = service(pet = pet, breed = null)

        assertFailsWith<IllegalStateException> { service.getPet(command(petId = pet.id!!)) }
    }

    private fun service(
        pet: Pet?,
        breed: BreedSummary? = BreedSummary(4L, "골든 리트리버", null),
    ) = GetPetService(
        requireUserId = RequireUserId(FakeLoadUserPort(userId = 1L)),
        loadPetPort = FakeLoadPetPort(pet),
        loadBreedPort = FakeLoadBreedPort(breed),
    )

    private fun command(petId: PetId) = GetPetCommand(userCode = UserCode("ABCD1234"), petId = petId)

    private fun pet(userId: Long = 1L) =
        Pet.reconstitute(
            id = PetId(1L),
            userId = UserId(userId),
            name = "호두",
            profileImage = null,
            relationship = Relationship.GUARDIAN,
            relationshipText = null,
            breedId = 4L,
            gender = Gender.MALE,
            birthYear = 2020,
            weight = 10.0,
            isNeutered = null,
            isRepresentative = false,
            deletedAt = null,
        )

    private class FakeLoadUserPort(
        private val userId: Long,
    ) : LoadUserPort {
        override fun findById(id: UserId): User? = null

        override fun findByCode(code: UserCode): User? =
            User.reconstitute(
                id = UserId(userId),
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

    private class FakeLoadPetPort(
        private val pet: Pet?,
    ) : LoadPetPort {
        override fun findById(id: PetId): Pet? = pet

        override fun findAllActiveByUserId(userId: UserId): List<Pet> = emptyList()

        override fun findAllActiveByUserIdForUpdate(userId: UserId): List<Pet> = emptyList()
    }

    private class FakeLoadBreedPort(
        private val breed: BreedSummary?,
    ) : LoadBreedPort {
        override fun findById(breedId: Long): BreedSummary? = breed
    }
}
