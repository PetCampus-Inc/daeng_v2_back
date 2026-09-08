package com.petcampus.knockdog.domain.pet.application.service

import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserPort
import com.petcampus.knockdog.domain.auth.domain.AddressType
import com.petcampus.knockdog.domain.auth.domain.User
import com.petcampus.knockdog.domain.auth.domain.UserAddress
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.auth.domain.UserId
import com.petcampus.knockdog.domain.pet.application.port.input.DeletePetCommand
import com.petcampus.knockdog.domain.pet.application.port.output.LoadPetPort
import com.petcampus.knockdog.domain.pet.application.port.output.SavePetPort
import com.petcampus.knockdog.domain.pet.domain.Gender
import com.petcampus.knockdog.domain.pet.domain.Pet
import com.petcampus.knockdog.domain.pet.domain.PetId
import com.petcampus.knockdog.domain.pet.domain.Relationship
import com.petcampus.knockdog.global.exception.BusinessException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeletePetServiceTest {
    @Test
    fun `대표견이 아닌 pet은 낙관적 락 저장만으로 삭제한다`() {
        val pet = pet(isRepresentative = false)
        val savePetPort = RecordingSavePetPort()
        val service = service(pet = pet, savePetPort = savePetPort)

        service.delete(command(petId = pet.id!!))

        assertTrue(pet.isDeleted)
        assertTrue(savePetPort.saveCalled)
        assertFalse(savePetPort.deleteAndPromoteWithinLockCalled)
    }

    @Test
    fun `대표견인 pet은 락 안에서 삭제·승격 경로를 탄다`() {
        val pet = pet(isRepresentative = true)
        val savePetPort = RecordingSavePetPort()
        val service = service(pet = pet, savePetPort = savePetPort)

        service.delete(command(petId = pet.id!!))

        assertTrue(savePetPort.deleteAndPromoteWithinLockCalled)
        assertFalse(savePetPort.saveCalled)
    }

    @Test
    fun `존재하지 않는 pet이면 NOT_FOUND를 던진다`() {
        val service = service(pet = null)

        assertFailsWith<BusinessException> { service.delete(command(petId = PetId(1L))) }
    }

    @Test
    fun `이미 삭제된 pet이면 NOT_FOUND를 던진다`() {
        val pet = pet()
        pet.delete()
        val service = service(pet = pet)

        assertFailsWith<BusinessException> { service.delete(command(petId = pet.id!!)) }
    }

    @Test
    fun `다른 사용자의 pet이면 NOT_AUTHORIZED를 던진다`() {
        val pet = pet(userId = 999L)
        val service = service(pet = pet)

        assertFailsWith<BusinessException> { service.delete(command(petId = pet.id!!)) }
    }

    private fun service(
        pet: Pet?,
        savePetPort: SavePetPort = RecordingSavePetPort(),
    ) = DeletePetService(
        loadUserPort = FakeLoadUserPort(userId = 1L),
        loadPetPort = FakeLoadPetPort(pet),
        savePetPort = savePetPort,
    )

    private fun command(petId: PetId) = DeletePetCommand(userCode = UserCode("ABCD1234"), petId = petId)

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

    private class RecordingSavePetPort : SavePetPort {
        var saveCalled = false
            private set
        var deleteAndPromoteWithinLockCalled = false
            private set

        override fun registerWithinLimit(pet: Pet): Pet = pet

        override fun save(pet: Pet): Pet {
            saveCalled = true
            return pet
        }

        override fun setRepresentativeWithinLock(pet: Pet): Pet = pet

        override fun deleteAndPromoteWithinLock(pet: Pet): Pet? {
            deleteAndPromoteWithinLockCalled = true
            return null
        }
    }
}
