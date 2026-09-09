package com.petcampus.knockdog.domain.pet.domain

import java.time.Year
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PetTest {
    @Test
    fun `relationship이 ETC이면 relationshipText가 없으면 생성에 실패한다`() {
        assertFailsWith<IllegalArgumentException> {
            pet(relationship = Relationship.ETC, relationshipText = null)
        }
    }

    @Test
    fun `relationship이 ETC이면 relationshipText가 공백이어도 생성에 실패한다`() {
        assertFailsWith<IllegalArgumentException> {
            pet(relationship = Relationship.ETC, relationshipText = "   ")
        }
    }

    @Test
    fun `relationship이 ETC가 아니면 relationshipText가 없어도 생성된다`() {
        val result = pet(relationship = Relationship.MOTHER, relationshipText = null)

        assertEquals(Relationship.MOTHER, result.relationship)
        assertNull(result.relationshipText)
    }

    @Test
    fun `relationship이 ETC가 아닌데 relationshipText가 있으면 생성에 실패한다`() {
        assertFailsWith<IllegalArgumentException> {
            pet(relationship = Relationship.MOTHER, relationshipText = "이모")
        }
    }

    @Test
    fun `relationship이 ETC이고 relationshipText가 100자를 초과하면 생성에 실패한다`() {
        assertFailsWith<IllegalArgumentException> {
            pet(relationship = Relationship.ETC, relationshipText = "가".repeat(101))
        }
    }

    @Test
    fun `name이 비어 있으면 생성에 실패한다`() {
        assertFailsWith<IllegalArgumentException> { pet(name = "") }
    }

    @Test
    fun `name이 공백뿐이면 생성에 실패한다`() {
        assertFailsWith<IllegalArgumentException> { pet(name = "   ") }
    }

    @Test
    fun `name이 100자를 초과하면 생성에 실패한다`() {
        assertFailsWith<IllegalArgumentException> { pet(name = "가".repeat(101)) }
    }

    @Test
    fun `name이 100자이면 생성된다`() {
        val result = pet(name = "가".repeat(100))

        assertEquals(100, result.name.length)
    }

    @Test
    fun `profileImage가 500자를 초과하면 생성에 실패한다`() {
        assertFailsWith<IllegalArgumentException> { pet(profileImage = "a".repeat(501)) }
    }

    @Test
    fun `profileImage가 500자이면 생성된다`() {
        val result = pet(profileImage = "a".repeat(500))

        assertEquals(500, result.profileImage?.length)
    }

    @Test
    fun `weight가 1 미만이면 생성에 실패한다`() {
        assertFailsWith<IllegalArgumentException> { pet(weight = 0.9) }
    }

    @Test
    fun `weight가 99 초과면 생성에 실패한다`() {
        assertFailsWith<IllegalArgumentException> { pet(weight = 99.1) }
    }

    @Test
    fun `weight가 1에서 99 사이면 생성된다`() {
        pet(weight = 1.0)
        pet(weight = 99.0)
    }

    @Test
    fun `weight에 소수점이 있으면 생성에 실패한다`() {
        assertFailsWith<IllegalArgumentException> { pet(weight = 45.5) }
    }

    @Test
    fun `birthYear가 최근 30년 이내면 생성된다`() {
        val currentYear = Year.now().value

        pet(birthYear = currentYear - 30)
        pet(birthYear = currentYear)
    }

    @Test
    fun `birthYear가 최근 30년보다 오래되면 생성에 실패한다`() {
        assertFailsWith<IllegalArgumentException> { pet(birthYear = Year.now().value - 31) }
    }

    @Test
    fun `birthYear가 미래면 생성에 실패한다`() {
        assertFailsWith<IllegalArgumentException> { pet(birthYear = Year.now().value + 1) }
    }

    @Test
    fun `birthYear가 없으면 생성된다`() {
        pet(birthYear = null)
    }

    @Test
    fun `수정 시 birthYear가 최근 30년보다 오래되면 실패한다`() {
        val pet = pet()

        assertFailsWith<IllegalArgumentException> {
            pet.update(
                name = pet.name,
                profileImage = pet.profileImage,
                relationship = pet.relationship,
                relationshipText = pet.relationshipText,
                breedId = pet.breedId,
                gender = pet.gender,
                birthYear = Year.now().value - 31,
                weight = pet.weight,
                isNeutered = pet.isNeutered,
            )
        }
    }

    @Test
    fun `markAsRepresentative 호출 시 대표견이 된다`() {
        val result = pet(isRepresentative = false)

        result.markAsRepresentative()

        assertTrue(result.isRepresentative)
    }

    @Test
    fun `clearRepresentative 호출 시 대표견이 해제된다`() {
        val result = pet(isRepresentative = true)

        result.clearRepresentative()

        assertFalse(result.isRepresentative)
    }

    @Test
    fun `delete 호출 시 삭제 상태가 된다`() {
        val result = pet()

        result.delete()

        assertTrue(result.isDeleted)
    }

    @Test
    fun `이미 삭제된 pet을 다시 삭제하면 실패한다`() {
        val result = pet()
        result.delete()

        assertFailsWith<IllegalStateException> { result.delete() }
    }

    @Test
    fun `삭제된 pet을 대표견으로 지정하면 실패한다`() {
        val result = pet()
        result.delete()

        assertFailsWith<IllegalStateException> { result.markAsRepresentative() }
    }

    @Test
    fun `대표견을 삭제하면 대표견 상태도 함께 해제된다`() {
        val result = pet(isRepresentative = true)

        result.delete()

        assertFalse(result.isRepresentative)
    }

    @Test
    fun `selectNextRepresentative는 후보 중 이름순으로 가장 앞선 pet을 고른다`() {
        val candidates = listOf(pet(name = "다롱"), pet(name = "가온"), pet(name = "나비"))

        val result = Pet.selectNextRepresentative(candidates)

        assertEquals("가온", result?.name)
    }

    @Test
    fun `selectNextRepresentative는 이미 대표견인 후보를 우선한다`() {
        val candidates = listOf(pet(name = "가온"), pet(name = "나비", isRepresentative = true))

        val result = Pet.selectNextRepresentative(candidates)

        assertEquals("나비", result?.name)
    }

    @Test
    fun `selectNextRepresentative는 후보가 없으면 null을 반환한다`() {
        val result = Pet.selectNextRepresentative(emptyList())

        assertNull(result)
    }

    @Test
    fun `hasReachedActiveLimit은 활성 pet이 최대 마릿수 미만이면 false를 반환한다`() {
        val activePets = List(4) { pet() }

        assertFalse(Pet.hasReachedActiveLimit(activePets))
    }

    @Test
    fun `hasReachedActiveLimit은 활성 pet이 최대 마릿수 이상이면 true를 반환한다`() {
        val activePets = List(5) { pet() }

        assertTrue(Pet.hasReachedActiveLimit(activePets))
    }

    @Test
    fun `assignRepresentativeIfFirst는 활성 pet이 없으면 대표견으로 지정한다`() {
        val result = pet(isRepresentative = false)

        result.assignRepresentativeIfFirst(emptyList())

        assertTrue(result.isRepresentative)
    }

    @Test
    fun `assignRepresentativeIfFirst는 활성 pet이 있으면 대표견으로 지정하지 않는다`() {
        val result = pet(isRepresentative = false)

        result.assignRepresentativeIfFirst(listOf(pet(isRepresentative = true)))

        assertFalse(result.isRepresentative)
    }

    @Test
    fun `reassignRepresentative는 이미 대표견이면 null을 반환하고 아무것도 바꾸지 않는다`() {
        val target = pet(isRepresentative = true)

        val result = Pet.reassignRepresentative(target, listOf(target))

        assertNull(result)
        assertTrue(target.isRepresentative)
    }

    @Test
    fun `reassignRepresentative는 기존 대표견을 해제하고 target을 대표견으로 지정한다`() {
        val previousRepresentative = pet(name = "이전", isRepresentative = true)
        val target = pet(name = "새로운", isRepresentative = false)

        val result = Pet.reassignRepresentative(target, listOf(previousRepresentative, target))

        assertEquals(listOf(previousRepresentative), result)
        assertFalse(previousRepresentative.isRepresentative)
        assertTrue(target.isRepresentative)
    }

    @Test
    fun `reassignRepresentative는 기존 대표견이 없어도 target을 대표견으로 지정한다`() {
        val target = pet(isRepresentative = false)

        val result = Pet.reassignRepresentative(target, listOf(target))

        assertEquals(emptyList(), result)
        assertTrue(target.isRepresentative)
    }

    @Test
    fun `promoteReplacement는 삭제된 pet이 대표견이 아니었으면 null을 반환한다`() {
        val result = Pet.promoteReplacement(wasRepresentative = false, remainingActivePets = listOf(pet(name = "가온")))

        assertNull(result)
    }

    @Test
    fun `promoteReplacement는 삭제된 pet이 대표견이었으면 남은 pet 중 이름순으로 승격한다`() {
        val first = pet(name = "가온")
        val second = pet(name = "나비")

        val result = Pet.promoteReplacement(wasRepresentative = true, remainingActivePets = listOf(second, first))

        assertEquals("가온", result?.name)
        assertTrue(first.isRepresentative)
        assertFalse(second.isRepresentative)
    }

    @Test
    fun `promoteReplacement는 삭제된 pet이 대표견이었어도 남은 pet이 없으면 null을 반환한다`() {
        val result = Pet.promoteReplacement(wasRepresentative = true, remainingActivePets = emptyList())

        assertNull(result)
    }

    private fun pet(
        name: String = "호두",
        profileImage: String? = null,
        relationship: Relationship = Relationship.GUARDIAN,
        relationshipText: String? = null,
        birthYear: Int? = 2020,
        weight: Double = 10.0,
        isRepresentative: Boolean = false,
    ) = Pet.create(
        userId = 1L,
        name = name,
        profileImage = profileImage,
        relationship = relationship,
        relationshipText = relationshipText,
        breedId = 1L,
        gender = Gender.MALE,
        birthYear = birthYear,
        weight = weight,
        isNeutered = null,
        isRepresentative = isRepresentative,
    )
}
