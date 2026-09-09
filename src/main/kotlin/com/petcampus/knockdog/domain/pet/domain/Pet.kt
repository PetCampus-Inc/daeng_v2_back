package com.petcampus.knockdog.domain.pet.domain

import java.time.LocalDateTime
import java.time.Year

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
    val version: Long,
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
        validateName(name)
        validateProfileImage(profileImage)
        validateRelationshipText(relationship, relationshipText)
        validateBirthYear(birthYear)
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

    fun assignRepresentativeIfFirst(activePets: List<Pet>) {
        if (activePets.isEmpty()) markAsRepresentative() else clearRepresentative()
    }

    fun delete() {
        check(!isDeleted) { "이미 삭제된 pet입니다." }
        isRepresentative = false
        deletedAt = LocalDateTime.now()
    }

    companion object {
        const val MAX_ACTIVE_COUNT = 5

        fun selectNextRepresentative(candidates: List<Pet>): Pet? =
            candidates.sortedWith(compareBy<Pet> { !it.isRepresentative }.thenBy { it.name }).firstOrNull()

        fun hasReachedActiveLimit(activePets: List<Pet>): Boolean = activePets.size >= MAX_ACTIVE_COUNT

        fun reassignRepresentative(
            target: Pet,
            activePets: List<Pet>,
        ): List<Pet>? {
            if (target.isRepresentative) return null

            val previouslyRepresentative = activePets.filter { it.isRepresentative }
            previouslyRepresentative.forEach { it.clearRepresentative() }
            target.markAsRepresentative()
            return previouslyRepresentative
        }

        fun promoteReplacement(
            wasRepresentative: Boolean,
            remainingActivePets: List<Pet>,
        ): Pet? {
            if (!wasRepresentative) return null
            return selectNextRepresentative(remainingActivePets)?.apply { markAsRepresentative() }
        }

        private val WEIGHT_RANGE = 1.0..99.0
        private const val NAME_MAX_LENGTH = 100
        private const val PROFILE_IMAGE_MAX_LENGTH = 500
        private const val RELATIONSHIP_TEXT_MAX_LENGTH = 100

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
            validateName(name)
            validateProfileImage(profileImage)
            validateRelationshipText(relationship, relationshipText)
            validateBirthYear(birthYear)
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
                version = 0,
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
            version: Long = 0,
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
                version,
            )

        private fun validateName(name: String) {
            require(name.isNotBlank()) { "name은 비어 있을 수 없습니다." }
            require(name.length <= NAME_MAX_LENGTH) { "name은 ${NAME_MAX_LENGTH}자를 초과할 수 없습니다." }
        }

        private fun validateProfileImage(profileImage: String?) {
            if (profileImage != null) {
                require(profileImage.length <= PROFILE_IMAGE_MAX_LENGTH) { "profileImage는 ${PROFILE_IMAGE_MAX_LENGTH}자를 초과할 수 없습니다." }
            }
        }

        private fun validateRelationshipText(
            relationship: Relationship,
            relationshipText: String?,
        ) {
            if (relationship == Relationship.ETC) {
                require(!relationshipText.isNullOrBlank()) { "relationship이 ETC이면 relationshipText가 필요합니다." }
                require(relationshipText.length <= RELATIONSHIP_TEXT_MAX_LENGTH) {
                    "relationshipText는 ${RELATIONSHIP_TEXT_MAX_LENGTH}자를 초과할 수 없습니다."
                }
            } else {
                require(relationshipText == null) { "relationship이 ETC가 아니면 relationshipText는 비워야 합니다." }
            }
        }

        private fun validateWeight(weight: Double) {
            require(weight in WEIGHT_RANGE) { "weight는 ${WEIGHT_RANGE.start}~${WEIGHT_RANGE.endInclusive} 범위여야 합니다." }
            require(weight % 1.0 == 0.0) { "weight는 소수점 없는 정수 값이어야 합니다." }
        }

        private fun validateBirthYear(birthYear: Int?) {
            if (birthYear != null) {
                val currentYear = Year.now().value
                require(birthYear in currentYear - BIRTH_YEAR_RANGE..currentYear) {
                    "birthYear는 최근 ${BIRTH_YEAR_RANGE}년 이내여야 합니다."
                }
            }
        }

        private const val BIRTH_YEAR_RANGE = 30
    }
}
