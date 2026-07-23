package com.petcampus.knockdog.domain.owner.adapter.outbound.persistence

import com.petcampus.knockdog.domain.owner.domain.OwnerStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "owner")
class OwnerJpaEntity(
    @Id
    @Column(name = "id", length = 36)
    val id: String,
    @Column(name = "email", nullable = false, unique = true)
    val email: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: OwnerStatus,
)
