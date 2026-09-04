package com.petcampus.knockdog.domain.breed.adapter.outbound.persistence

import com.petcampus.knockdog.global.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "breeds")
class BreedJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
    @Column(name = "display_order", nullable = false)
    val displayOrder: Int,
    @Column(name = "fci_standard_number")
    val fciStandardNumber: Int?,
    @Column(name = "name_en", nullable = false, length = 255)
    val nameEn: String,
    @Column(name = "name_ko", nullable = false, length = 255)
    val nameKo: String,
    @Column(name = "alias", length = 255)
    val alias: String?,
) : BaseEntity()
