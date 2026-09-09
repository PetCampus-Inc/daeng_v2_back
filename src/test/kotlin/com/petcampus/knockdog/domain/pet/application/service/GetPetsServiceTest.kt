package com.petcampus.knockdog.domain.pet.application.service

import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserPort
import com.petcampus.knockdog.domain.auth.application.service.RequireUserId
import com.petcampus.knockdog.domain.auth.domain.AddressType
import com.petcampus.knockdog.domain.auth.domain.User
import com.petcampus.knockdog.domain.auth.domain.UserAddress
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.auth.domain.UserId
import com.petcampus.knockdog.domain.pet.application.port.input.GetPetsCommand
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
import kotlin.test.assertTrue

class GetPetsServiceTest {
    @Test
    fun `본인 소유 활성 pet 목록을 breed 정보와 함께 반환한다`() {
        val service = service(pets = listOf(pet(id = 1L, name = "호두")))

        val result = service.getPets(command())

        assertEquals(1, result.pets.size)
        assertEquals("호두", result.pets[0].pet.name)
        assertEquals("골든 리트리버", result.pets[0].breed.nameKo)
    }

    @Test
    fun `대표견이 가장 먼저 오고 나머지는 이름 오름차순이다`() {
        val service =
            service(
                pets =
                    listOf(
                        pet(id = 1L, name = "다롱이", isRepresentative = false),
                        pet(id = 2L, name = "가을이", isRepresentative = true),
                        pet(id = 3L, name = "나비", isRepresentative = false),
                    ),
            )

        val result = service.getPets(command())

        assertEquals(listOf("가을이", "나비", "다롱이"), result.pets.map { it.pet.name })
    }

    @Test
    fun `pet이 없으면 빈 목록을 반환한다`() {
        val service = service(pets = emptyList())

        val result = service.getPets(command())

        assertTrue(result.pets.isEmpty())
    }

    @Test
    fun `존재하지 않는 사용자면 NOT_FOUND_USER를 던진다`() {
        val service = service(pets = emptyList(), userId = null)

        assertFailsWith<BusinessException> { service.getPets(command()) }
    }

    @Test
    fun `참조하는 breed가 없으면 500으로 이어지는 예외를 던진다`() {
        val service = service(pets = listOf(pet(id = 1L, name = "호두")), breed = null)

        assertFailsWith<IllegalStateException> { service.getPets(command()) }
    }

    private fun service(
        pets: List<Pet>,
        userId: Long? = 1L,
        breed: BreedSummary? = BreedSummary(4L, "골든 리트리버", null),
    ) = GetPetsService(
        requireUserId = RequireUserId(FakeLoadUserPort(userId)),
        loadPetPort = FakeLoadPetPort(pets),
        loadBreedPort = FakeLoadBreedPort(breed),
    )

    private fun command() = GetPetsCommand(userCode = UserCode("ABCD1234"))

    private fun pet(
        id: Long,
        name: String,
        isRepresentative: Boolean = false,
    ) = Pet.reconstitute(
        id = PetId(id),
        userId = UserId(1L),
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

    private class FakeLoadPetPort(
        private val pets: List<Pet>,
    ) : LoadPetPort {
        override fun findById(id: PetId): Pet? = pets.find { it.id == id }

        override fun findAllActiveByUserId(userId: UserId): List<Pet> = pets

        override fun findAllActiveByUserIdForUpdate(userId: UserId): List<Pet> = pets
    }

    private class FakeLoadBreedPort(
        private val breed: BreedSummary?,
    ) : LoadBreedPort {
        override fun findById(breedId: Long): BreedSummary? = breed
    }
}
