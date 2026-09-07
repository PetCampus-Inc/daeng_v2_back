package com.petcampus.knockdog.domain.pet.application.service

import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserPort
import com.petcampus.knockdog.domain.auth.domain.AddressType
import com.petcampus.knockdog.domain.auth.domain.User
import com.petcampus.knockdog.domain.auth.domain.UserAddress
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.auth.domain.UserId
import com.petcampus.knockdog.domain.pet.application.port.input.SetRepresentativeCommand
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
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SetRepresentativeServiceTest {
    @Test
    fun `대표견이 아니던 pet을 대표견으로 설정한다`() {
        val pet = pet(isRepresentative = false)
        val savePetPort = RecordingSavePetPort()
        val service = service(pet = pet, savePetPort = savePetPort)

        val result = service.setRepresentative(command(petId = pet.id!!))

        assertTrue(result.pet.isRepresentative)
        assertTrue(savePetPort.setRepresentativeWithinLockCalled)
    }

    @Test
    fun `이미 대표견인 pet을 다시 설정해도 대표견 상태를 유지한다`() {
        val pet = pet(isRepresentative = true)
        val service = service(pet = pet)

        val result = service.setRepresentative(command(petId = pet.id!!))

        assertTrue(result.pet.isRepresentative)
    }

    @Test
    fun `존재하지 않는 pet이면 NOT_FOUND를 던진다`() {
        val service = service(pet = null)

        assertFailsWith<BusinessException> { service.setRepresentative(command(petId = PetId(1L))) }
    }

    @Test
    fun `삭제된 pet이면 NOT_FOUND를 던진다`() {
        val pet = pet()
        pet.delete()
        val service = service(pet = pet)

        assertFailsWith<BusinessException> { service.setRepresentative(command(petId = pet.id!!)) }
    }

    @Test
    fun `다른 사용자의 pet이면 NOT_AUTHORIZED를 던진다`() {
        val pet = pet(userId = 999L)
        val service = service(pet = pet)

        assertFailsWith<BusinessException> { service.setRepresentative(command(petId = pet.id!!)) }
    }

    private fun service(
        pet: Pet?,
        breed: BreedSummary? = BreedSummary(4L, "골든 리트리버", null),
        savePetPort: SavePetPort = RecordingSavePetPort(),
    ) = SetRepresentativeService(
        loadUserPort = FakeLoadUserPort(userId = 1L),
        loadPetPort = FakeLoadPetPort(pet),
        loadBreedPort = FakeLoadBreedPort(breed),
        savePetPort = savePetPort,
    )

    private fun command(petId: PetId) = SetRepresentativeCommand(userCode = UserCode("ABCD1234"), petId = petId)

    private fun pet(
        userId: Long = 1L,
        isRepresentative: Boolean = false,
    ) = Pet.reconstitute(
        id = PetId(1L),
        userId = userId,
        name = "호두",
        profileImage = null,
        relationship = Relationship.GUARDIAN,
        relationshipText = null,
        breedId = 4L,
        gender = Gender.MALE,
        birthYear = 2020,
        weight = 10.0,
        isNeutered = null,
        isRepresentative = isRepresentative,
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

        override fun findAllActiveByUserId(userId: Long): List<Pet> = emptyList()
    }

    private class FakeLoadBreedPort(
        private val breed: BreedSummary?,
    ) : LoadBreedPort {
        override fun findById(breedId: Long): BreedSummary? = breed
    }

    private class RecordingSavePetPort : SavePetPort {
        var setRepresentativeWithinLockCalled = false
            private set

        override fun registerWithinLimit(pet: Pet): Pet = pet

        override fun save(pet: Pet): Pet = pet

        override fun setRepresentativeWithinLock(pet: Pet): Pet {
            setRepresentativeWithinLockCalled = true
            if (!pet.isRepresentative) pet.markAsRepresentative()
            return pet
        }
    }
}
