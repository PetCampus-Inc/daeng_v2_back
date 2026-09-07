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
@Table(name = "kindergarten_menus")
class KindergartenMenuJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
    @Column(name = "kindergarten_id", nullable = false)
    val kindergartenId: Long,
    @Column(name = "product_type", nullable = false, length = 30)
    val productType: String,
    @Column(name = "service_type", nullable = false, length = 30)
    val serviceType: String,
    @Column(name = "product_name", nullable = false, length = 200)
    val productName: String,
    @Column(name = "unit")
    val unit: Double?,
    @Column(name = "unit_label", length = 50)
    val unitLabel: String?,
    @Column(name = "unit_type", length = 20)
    val unitType: String?,
    @Column(name = "weight_range", length = 50)
    val weightRange: String?,
    @Column(name = "price")
    val price: Int?,
    @Column(name = "hourly_price")
    val hourlyPrice: Int?,
    @Column(name = "is_min_price", nullable = false)
    val isMinPrice: Boolean,
    @Column(name = "is_max_price", nullable = false)
    val isMaxPrice: Boolean,
    @Column(name = "total_duration_label", length = 100)
    val totalDurationLabel: String?,
    @Column(name = "total_duration_minutes")
    val totalDurationMinutes: Int?,
    @Column(name = "display_order", nullable = false)
    val displayOrder: Int,
    deletedAt: LocalDateTime? = null,
) : BaseEntity(deletedAt)
