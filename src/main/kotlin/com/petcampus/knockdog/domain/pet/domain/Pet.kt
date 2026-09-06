package com.petcampus.knockdog.domain.pet.domain

import java.time.LocalDateTime

class Pet private constructor(
    val id: PetId?,
    val userId: Long,
    name: String,
    profileImage: String?,
    relationship: Relationship,
    relationshipText: String?,
    breedId: Long,
    gender: Gender,
    birthYear: Int?,
    weight: Double,
    isNeutered: Boolean?,
    isRepresentative: Boolean,
    deletedAt: LocalDateTime?,
) {
    var name: String = name
        private set

    var profileImage: String? = profileImage
        private set

    var relationship: Relationship = relationship
        private set

    var relationshipText: String? = relationshipText
        private set

    var breedId: Long = breedId
        private set

    var gender: Gender = gender
        private set

    var birthYear: Int? = birthYear
        private set

    var weight: Double = weight
        private set

    var isNeutered: Boolean? = isNeutered
        private set

    var isRepresentative: Boolean = isRepresentative
        private set

    var deletedAt: LocalDateTime? = deletedAt
        private set

    val isDeleted: Boolean
        get() = deletedAt != null

    fun update(
        name: String,
        profileImage: String?,
        relationship: Relationship,
        relationshipText: String?,
        breedId: Long,
        gender: Gender,
        birthYear: Int?,
        weight: Double,
        isNeutered: Boolean?,
    ) {
        validateRelationshipText(relationship, relationshipText)
        validateWeight(weight)

        this.name = name
        this.profileImage = profileImage
        this.relationship = relationship
        this.relationshipText = relationshipText
        this.breedId = breedId
        this.gender = gender
        this.birthYear = birthYear
        this.weight = weight
        this.isNeutered = isNeutered
    }

    fun markAsRepresentative() {
        check(!isDeleted) { "삭제된 pet은 대표견으로 지정할 수 없습니다." }
        isRepresentative = true
    }

    fun clearRepresentative() {
        isRepresentative = false
    }

    fun delete() {
        check(!isDeleted) { "이미 삭제된 pet입니다." }
        isRepresentative = false
        deletedAt = LocalDateTime.now()
    }

    companion object {
        const val MAX_ACTIVE_COUNT = 5

        private val WEIGHT_RANGE = 1.0..99.0

        fun create(
            userId: Long,
            name: String,
            profileImage: String?,
            relationship: Relationship,
            relationshipText: String?,
            breedId: Long,
            gender: Gender,
            birthYear: Int?,
            weight: Double,
            isNeutered: Boolean?,
            isRepresentative: Boolean,
        ): Pet {
            validateRelationshipText(relationship, relationshipText)
            validateWeight(weight)

            return Pet(
                id = null,
                userId = userId,
                name = name,
                profileImage = profileImage,
                relationship = relationship,
                relationshipText = relationshipText,
                breedId = breedId,
                gender = gender,
                birthYear = birthYear,
                weight = weight,
                isNeutered = isNeutered,
                isRepresentative = isRepresentative,
                deletedAt = null,
            )
        }

        fun reconstitute(
            id: PetId,
            userId: Long,
            name: String,
            profileImage: String?,
            relationship: Relationship,
            relationshipText: String?,
            breedId: Long,
            gender: Gender,
            birthYear: Int?,
            weight: Double,
            isNeutered: Boolean?,
            isRepresentative: Boolean,
            deletedAt: LocalDateTime?,
        ): Pet =
            Pet(
                id,
                userId,
                name,
                profileImage,
                relationship,
                relationshipText,
                breedId,
                gender,
                birthYear,
                weight,
                isNeutered,
                isRepresentative,
                deletedAt,
            )

        private fun validateRelationshipText(
            relationship: Relationship,
            relationshipText: String?,
        ) {
            if (relationship == Relationship.ETC) {
                require(!relationshipText.isNullOrBlank()) { "relationship이 ETC이면 relationshipText가 필요합니다." }
            } else {
                require(relationshipText == null) { "relationship이 ETC가 아니면 relationshipText는 비워야 합니다." }
            }
        }

        private fun validateWeight(weight: Double) {
            require(weight in WEIGHT_RANGE) { "weight는 ${WEIGHT_RANGE.start}~${WEIGHT_RANGE.endInclusive} 범위여야 합니다." }
            require(weight % 1.0 == 0.0) { "weight는 소수점 없는 정수 값이어야 합니다." }
        }
    }
}
