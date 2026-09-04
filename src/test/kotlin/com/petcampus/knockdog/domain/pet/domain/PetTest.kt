package com.petcampus.knockdog.domain.pet.domain

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
    fun `대표견을 삭제하면 대표견 상태도 함께 해제된다`() {
        val result = pet(isRepresentative = true)

        result.delete()

        assertFalse(result.isRepresentative)
    }

    private fun pet(
        relationship: Relationship = Relationship.GUARDIAN,
        relationshipText: String? = null,
        weight: Double = 10.0,
        isRepresentative: Boolean = false,
    ) = Pet.create(
        userId = 1L,
        name = "호두",
        profileImage = null,
        relationship = relationship,
        relationshipText = relationshipText,
        breedId = 1L,
        gender = Gender.MALE,
        birthYear = 2020,
        weight = weight,
        isNeutered = null,
        isRepresentative = isRepresentative,
    )
}
