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
@Table(name = "kindergarten_price_images")
class KindergartenPriceImageJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
    @Column(name = "kindergarten_id", nullable = false)
    val kindergartenId: Long,
    @Column(name = "s3_key", nullable = false, length = 512)
    val s3Key: String,
    @Column(name = "display_order", nullable = false)
    val displayOrder: Int,
    deletedAt: LocalDateTime? = null,
) : BaseEntity(deletedAt)
