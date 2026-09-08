package com.petcampus.knockdog.domain.pet.application.service

import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserPort
import com.petcampus.knockdog.domain.auth.domain.AddressType
import com.petcampus.knockdog.domain.auth.domain.User
import com.petcampus.knockdog.domain.auth.domain.UserAddress
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.auth.domain.UserId
import com.petcampus.knockdog.domain.pet.application.port.input.UpdatePetCommand
import com.petcampus.knockdog.domain.pet.application.port.output.BreedSummary
import com.petcampus.knockdog.domain.pet.application.port.output.LoadBreedPort
import com.petcampus.knockdog.domain.pet.application.port.output.LoadPetPort
import com.petcampus.knockdog.domain.pet.application.port.output.SavePetPort
import com.petcampus.knockdog.domain.pet.domain.Gender
import com.petcampus.knockdog.domain.pet.domain.Pet
import com.petcampus.knockdog.domain.pet.domain.PetId
import com.petcampus.knockdog.domain.pet.domain.Relationship
import com.petcampus.knockdog.global.exception.BusinessException
import org.openapitools.jackson.nullable.JsonNullable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class UpdatePetServiceTest {
    @Test
    fun `생략한 필드는 기존 값을 유지한다`() {
        val pet = pet(name = "호두", weight = 10.0)
        val service = service(pet = pet)

        val result = service.update(command(petId = pet.id!!))

        assertEquals("호두", result.pet.name)
        assertEquals(10.0, result.pet.weight)
    }

    @Test
    fun `nullable 필드에 명시적 null을 보내면 지운다`() {
        val pet = pet(profileImage = "https://example.com/dog.png")
        val service = service(pet = pet)

        val result = service.update(command(petId = pet.id!!, profileImage = JsonNullable.of(null)))

        assertNull(result.pet.profileImage)
    }

    @Test
    fun `weight에 명시적 null을 보내면 거부된다`() {
        val pet = pet(weight = 10.0)
        val service = service(pet = pet)

        assertFailsWith<IllegalArgumentException> {
            service.update(command(petId = pet.id!!, weight = JsonNullable.of(null)))
        }
    }

    @Test
    fun `breedId에 명시적 null을 보내면 거부된다`() {
        val pet = pet()
        val service = service(pet = pet)

        assertFailsWith<IllegalArgumentException> {
            service.update(command(petId = pet.id!!, breedId = JsonNullable.of(null)))
        }
    }

    @Test
    fun `relationship에 명시적 null을 보내면 거부된다`() {
        val pet = pet()
        val service = service(pet = pet)

        assertFailsWith<IllegalArgumentException> {
            service.update(command(petId = pet.id!!, relationship = JsonNullable.of(null)))
        }
    }

    @Test
    fun `gender에 명시적 null을 보내면 거부된다`() {
        val pet = pet()
        val service = service(pet = pet)

        assertFailsWith<IllegalArgumentException> {
            service.update(command(petId = pet.id!!, gender = JsonNullable.of(null)))
        }
    }

    @Test
    fun `name에 명시적 null을 보내면 거부된다`() {
        val pet = pet()
        val service = service(pet = pet)

        assertFailsWith<IllegalArgumentException> {
            service.update(command(petId = pet.id!!, name = JsonNullable.of(null)))
        }
    }

    @Test
    fun `존재하지 않는 pet이면 NOT_FOUND를 던진다`() {
        val service = service(pet = null)

        assertFailsWith<BusinessException> { service.update(command(petId = PetId(1L))) }
    }

    @Test
    fun `삭제된 pet이면 NOT_FOUND를 던진다`() {
        val pet = pet()
        pet.delete()
        val service = service(pet = pet)

        assertFailsWith<BusinessException> { service.update(command(petId = pet.id!!)) }
    }

    @Test
    fun `다른 사용자의 pet이면 NOT_AUTHORIZED를 던진다`() {
        val pet = pet(userId = 999L)
        val service = service(pet = pet)

        assertFailsWith<BusinessException> { service.update(command(petId = pet.id!!)) }
    }

    @Test
    fun `존재하지 않는 breedId로 바꾸면 NOT_FOUND_BREED를 던진다`() {
        val pet = pet()
        val service = service(pet = pet, breed = null)

        assertFailsWith<BusinessException> {
            service.update(command(petId = pet.id!!, breedId = JsonNullable.of(999L)))
        }
    }

    @Test
    fun `relationship을 ETC가 아닌 값으로 바꾸면 relationshipText가 자동으로 지워진다`() {
        val pet = pet(relationship = Relationship.ETC, relationshipText = "이모")
        val service = service(pet = pet)

        val result = service.update(command(petId = pet.id!!, relationship = JsonNullable.of(Relationship.MOTHER)))

        assertNull(result.pet.relationshipText)
    }

    @Test
    fun `relationship을 ETC가 아닌 값으로 바꾸면서 relationshipText를 함께 보내면 거부된다`() {
        val pet = pet(relationship = Relationship.ETC, relationshipText = "이모")
        val service = service(pet = pet)

        assertFailsWith<IllegalArgumentException> {
            service.update(
                command(
                    petId = pet.id!!,
                    relationship = JsonNullable.of(Relationship.MOTHER),
                    relationshipText = JsonNullable.of("이모"),
                ),
            )
        }
    }

    @Test
    fun `relationship이 이미 ETC가 아닌 상태에서 relationshipText만 명시적으로 보내면 거부된다`() {
        val pet = pet(relationship = Relationship.GUARDIAN, relationshipText = null)
        val service = service(pet = pet)

        assertFailsWith<IllegalArgumentException> {
            service.update(command(petId = pet.id!!, relationshipText = JsonNullable.of("이모")))
        }
    }

    private fun service(
        pet: Pet?,
        breed: BreedSummary? = BreedSummary(4L, "골든 리트리버", null),
    ) = UpdatePetService(
        loadUserPort = FakeLoadUserPort(userId = 1L),
        loadPetPort = FakeLoadPetPort(pet),
        loadBreedPort = FakeLoadBreedPort(breed),
        savePetPort = FakeSavePetPort(),
    )

    private fun pet(
        userId: Long = 1L,
        name: String = "호두",
        profileImage: String? = null,
        relationship: Relationship = Relationship.GUARDIAN,
        relationshipText: String? = null,
        weight: Double = 10.0,
    ) = Pet.reconstitute(
        id = PetId(1L),
        userId = userId,
        name = name,
        profileImage = profileImage,
        relationship = relationship,
        relationshipText = relationshipText,
        breedId = 4L,
        gender = Gender.MALE,
        birthYear = 2020,
        weight = weight,
        isNeutered = null,
        isRepresentative = false,
        deletedAt = null,
    )

    private fun command(
        petId: PetId,
        name: JsonNullable<String> = JsonNullable.undefined(),
        profileImage: JsonNullable<String?> = JsonNullable.undefined(),
        relationship: JsonNullable<Relationship> = JsonNullable.undefined(),
        relationshipText: JsonNullable<String?> = JsonNullable.undefined(),
        breedId: JsonNullable<Long> = JsonNullable.undefined(),
        gender: JsonNullable<Gender> = JsonNullable.undefined(),
        weight: JsonNullable<Double> = JsonNullable.undefined(),
    ) = UpdatePetCommand(
        userCode = UserCode("ABCD1234"),
        petId = petId,
        name = name,
        profileImage = profileImage,
        relationship = relationship,
        relationshipText = relationshipText,
        breedId = breedId,
        gender = gender,
        weight = weight,
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

    private class FakeSavePetPort : SavePetPort {
        override fun registerWithinLimit(pet: Pet): Pet = pet

        override fun save(pet: Pet): Pet = pet

        override fun setRepresentativeWithinLock(pet: Pet): Pet = pet
    }
}
