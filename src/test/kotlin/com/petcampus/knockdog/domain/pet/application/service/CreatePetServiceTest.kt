package com.petcampus.knockdog.domain.pet.application.service

import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserPort
import com.petcampus.knockdog.domain.auth.domain.AddressType
import com.petcampus.knockdog.domain.auth.domain.User
import com.petcampus.knockdog.domain.auth.domain.UserAddress
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.auth.domain.UserId
import com.petcampus.knockdog.domain.pet.application.port.input.CreatePetCommand
import com.petcampus.knockdog.domain.pet.application.port.output.BreedSummary
import com.petcampus.knockdog.domain.pet.application.port.output.LoadBreedPort
import com.petcampus.knockdog.domain.pet.application.port.output.SavePetPort
import com.petcampus.knockdog.domain.pet.domain.Gender
import com.petcampus.knockdog.domain.pet.domain.Pet
import com.petcampus.knockdog.domain.pet.domain.Relationship
import com.petcampus.knockdog.global.exception.BusinessException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CreatePetServiceTest {
    @Test
    fun `정상 생성 시 breed 정보를 포함한 결과를 반환한다`() {
        val port = FakeSavePetPort()
        val service =
            CreatePetService(
                loadUserPort = FakeLoadUserPort(userId = 1L),
                loadBreedPort = FakeLoadBreedPort(BreedSummary(4L, "골든 리트리버", null)),
                savePetPort = port,
            )

        val result = service.create(command(breedId = 4L))

        assertEquals("골든 리트리버", result.breed.nameKo)
        assertEquals(1L, result.pet.userId)
    }

    @Test
    fun `존재하지 않는 사용자면 NOT_FOUND_USER를 던진다`() {
        val service =
            CreatePetService(
                loadUserPort = FakeLoadUserPort(userId = null),
                loadBreedPort = FakeLoadBreedPort(BreedSummary(4L, "골든 리트리버", null)),
                savePetPort = FakeSavePetPort(),
            )

        assertFailsWith<BusinessException> { service.create(command(breedId = 4L)) }
    }

    @Test
    fun `존재하지 않는 breedId면 NOT_FOUND_BREED를 던진다`() {
        val service =
            CreatePetService(
                loadUserPort = FakeLoadUserPort(userId = 1L),
                loadBreedPort = FakeLoadBreedPort(null),
                savePetPort = FakeSavePetPort(),
            )

        assertFailsWith<BusinessException> { service.create(command(breedId = 999L)) }
    }

    @Test
    fun `최대 마릿수 초과 시 LIMIT_EXCEEDED로 변환한다`() {
        val service =
            CreatePetService(
                loadUserPort = FakeLoadUserPort(userId = 1L),
                loadBreedPort = FakeLoadBreedPort(BreedSummary(4L, "골든 리트리버", null)),
                savePetPort = FakeSavePetPort(throwOnRegister = true),
            )

        assertFailsWith<BusinessException> { service.create(command(breedId = 4L)) }
    }

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

    private class FakeSavePetPort(
        private val throwOnRegister: Boolean = false,
    ) : SavePetPort {
        override fun registerWithinLimit(pet: Pet): Pet {
            if (throwOnRegister) throw IllegalStateException("최대 마릿수를 초과했습니다.")
            return pet
        }

        override fun save(pet: Pet): Pet = pet
    }
}
