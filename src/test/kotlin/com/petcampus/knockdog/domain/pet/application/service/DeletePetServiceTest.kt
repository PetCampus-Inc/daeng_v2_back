package com.petcampus.knockdog.domain.pet.application.service

import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserPort
import com.petcampus.knockdog.domain.auth.application.port.output.LockUserPort
import com.petcampus.knockdog.domain.auth.domain.AddressType
import com.petcampus.knockdog.domain.auth.domain.User
import com.petcampus.knockdog.domain.auth.domain.UserAddress
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.auth.domain.UserId
import com.petcampus.knockdog.domain.pet.application.PetErrorCode
import com.petcampus.knockdog.domain.pet.application.port.input.DeletePetCommand
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeletePetServiceTest {
    @Test
    fun `대표견이 아닌 pet은 낙관적 락 저장만으로 삭제한다`() {
        val pet = pet(id = 1L, isRepresentative = false)
        val savePetPort = RecordingSavePetPort()
        val service = service(pet = pet, activePets = listOf(pet), savePetPort = savePetPort)

        service.delete(command(petId = pet.id!!))

        assertTrue(pet.isDeleted)
        assertTrue(savePetPort.saveCalled)
        assertFalse(savePetPort.saveAndFlushCalled)
    }

    @Test
    fun `대표견을 삭제하면 남은 pet 중 이름순으로 다음 pet이 승격된다`() {
        val target = pet(id = 1L, name = "다롱", isRepresentative = true)
        val first = pet(id = 2L, name = "가온", isRepresentative = false)
        val second = pet(id = 3L, name = "나비", isRepresentative = false)
        val savePetPort = RecordingSavePetPort()
        val service = service(pet = target, activePets = listOf(target, first, second), savePetPort = savePetPort)

        service.delete(command(petId = target.id!!))

        assertTrue(target.isDeleted)
        assertTrue(savePetPort.saveAndFlushCalled)
        assertTrue(first.isRepresentative)
        assertFalse(second.isRepresentative)
    }

    @Test
    fun `대표견을 삭제했는데 남은 pet이 없으면 승격이 일어나지 않는다`() {
        val target = pet(id = 1L, isRepresentative = true)
        val savePetPort = RecordingSavePetPort()
        val service = service(pet = target, activePets = listOf(target), savePetPort = savePetPort)

        service.delete(command(petId = target.id!!))

        assertTrue(target.isDeleted)
        assertNull(savePetPort.lastSavedNonTarget)
    }

    @Test
    fun `존재하지 않는 pet이면 NOT_FOUND를 던진다`() {
        val service = service(pet = null, activePets = emptyList())

        val exception = assertFailsWith<BusinessException> { service.delete(command(petId = PetId(1L))) }

        assertEquals(PetErrorCode.NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `이미 삭제된 pet이면 NOT_FOUND를 던진다`() {
        val pet = pet()
        pet.delete()
        val service = service(pet = pet, activePets = emptyList())

        val exception = assertFailsWith<BusinessException> { service.delete(command(petId = pet.id!!)) }

        assertEquals(PetErrorCode.NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `잠금 재조회 시점에 이미 삭제된 pet이면 NOT_FOUND를 던진다`() {
        val pet = pet(id = 1L, isRepresentative = true)
        val service = service(pet = pet, activePets = emptyList())

        val exception = assertFailsWith<BusinessException> { service.delete(command(petId = pet.id!!)) }

        assertEquals(PetErrorCode.NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `다른 사용자의 pet이면 NOT_AUTHORIZED를 던진다`() {
        val pet = pet(userId = 999L)
        val service = service(pet = pet, activePets = listOf(pet))

        val exception = assertFailsWith<BusinessException> { service.delete(command(petId = pet.id!!)) }

        assertEquals(PetErrorCode.NOT_AUTHORIZED, exception.errorCode)
    }

    private fun service(
        pet: Pet?,
        activePets: List<Pet>,
        savePetPort: SavePetPort = RecordingSavePetPort(),
    ) = DeletePetService(
        loadUserPort = FakeLoadUserPort(userId = 1L),
        loadPetPort = FakeLoadPetPort(pet, activePets),
        savePetPort = savePetPort,
        petLockOperations = PetLockOperations(NoopLockUserPort(), FakeLoadPetPort(pet, activePets)),
    )

    private fun command(petId: PetId) = DeletePetCommand(userCode = UserCode("ABCD1234"), petId = petId)

    private fun pet(
        id: Long = 1L,
        userId: Long = 1L,
        name: String = "호두",
        isRepresentative: Boolean = false,
    ) = Pet.reconstitute(
        id = PetId(id),
        userId = UserId(userId),
        name = name,
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
        private val activePets: List<Pet>,
    ) : LoadPetPort {
        override fun findById(id: PetId): Pet? = pet

        override fun findAllActiveByUserId(userId: UserId): List<Pet> = activePets

        override fun findAllActiveByUserIdForUpdate(userId: UserId): List<Pet> = activePets
    }

    private class NoopLockUserPort : LockUserPort {
        override fun lockById(userId: UserId) = Unit
    }

    private class RecordingSavePetPort : SavePetPort {
        var saveCalled = false
            private set
        var saveAndFlushCalled = false
            private set
        var lastSavedNonTarget: Pet? = null
            private set

        override fun save(pet: Pet): Pet {
            saveCalled = true
            if (!pet.isDeleted) lastSavedNonTarget = pet
            return pet
        }

        override fun saveAndFlush(pet: Pet): Pet {
            saveAndFlushCalled = true
            return pet
        }
    }
}
