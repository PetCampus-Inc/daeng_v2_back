package com.petcampus.knockdog.domain.comparison.adapter.outbound.persistence

import com.petcampus.knockdog.global.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "comparison_histories",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_code", "kindergarten_id_a", "kindergarten_id_b"])],
)
class ComparisonHistoryJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
    @Column(name = "user_code", nullable = false, length = 8)
    val userCode: String,
    @Column(name = "kindergarten_id_a", nullable = false, length = 100)
    val kindergartenIdA: String,
    @Column(name = "kindergarten_id_b", nullable = false, length = 100)
    val kindergartenIdB: String,
) : BaseEntity()
