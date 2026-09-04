package com.petcampus.knockdog.domain.pet.adapter.outbound.persistence

import com.petcampus.knockdog.domain.auth.adapter.outbound.persistence.UserJpaEntity
import com.petcampus.knockdog.domain.breed.adapter.outbound.persistence.BreedJpaEntity
import com.petcampus.knockdog.domain.pet.domain.Gender
import com.petcampus.knockdog.domain.pet.domain.Relationship
import com.petcampus.knockdog.global.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "pets",
    uniqueConstraints = [UniqueConstraint(name = "uk_pets_representative_user", columnNames = ["representative_user_id"])],
)
class PetJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val user: UserJpaEntity,
    @Column(name = "name", nullable = false, length = 100)
    val name: String,
    @Column(name = "profile_image", length = 500)
    val profileImage: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "relationship", nullable = false, length = 20)
    val relationship: Relationship,
    @Column(name = "relationship_text", length = 100)
    val relationshipText: String? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breed_id", nullable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val breed: BreedJpaEntity,
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    val gender: Gender,
    @Column(name = "birth_year")
    val birthYear: Int? = null,
    @Column(name = "weight")
    val weight: Double? = null,
    @Column(name = "is_neutered")
    val isNeutered: Boolean? = null,
    @Column(name = "representative_user_id")
    val representativeUserId: Long? = null,
    deletedAt: LocalDateTime? = null,
) : BaseEntity(deletedAt)
