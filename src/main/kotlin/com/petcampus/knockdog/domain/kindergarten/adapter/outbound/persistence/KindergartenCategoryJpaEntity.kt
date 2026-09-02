package com.petcampus.knockdog.domain.kindergarten.adapter.outbound.persistence

import com.petcampus.knockdog.global.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "kindergarten_categories")
class KindergartenCategoryJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
    @Column(name = "kindergarten_id", nullable = false)
    val kindergartenId: Long,
    @Column(name = "category", nullable = false, length = 30)
    val category: String,
    deletedAt: LocalDateTime? = null,
) : BaseEntity(deletedAt)
