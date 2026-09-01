package com.petcampus.knockdog.domain.auth.adapter.outbound.persistence

import com.petcampus.knockdog.domain.auth.domain.AddressType
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
import java.time.LocalDateTime

/** user_addresses.user_id: 실무 관행에 따라 연관관계 매핑은 유지하되 DB FK 제약은 걸지 않는다 (docs/conventions/jpa-entity.md). */
@Entity
@Table(name = "user_addresses")
class UserAddressJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val user: UserJpaEntity,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    val type: AddressType,
    @Column(name = "alias", length = 20)
    val alias: String? = null,
    @Column(name = "address", nullable = false, length = 200)
    val address: String,
    @Column(name = "road_address", length = 200)
    val roadAddress: String? = null,
    @Column(name = "address_detail", length = 100)
    val addressDetail: String? = null,
    @Column(name = "lat", nullable = false)
    val lat: Double,
    @Column(name = "lng", nullable = false)
    val lng: Double,
    deletedAt: LocalDateTime? = null,
) : BaseEntity(deletedAt)
