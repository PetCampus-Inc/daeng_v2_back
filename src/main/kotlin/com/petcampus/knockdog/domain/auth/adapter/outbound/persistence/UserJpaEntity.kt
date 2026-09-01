package com.petcampus.knockdog.domain.auth.adapter.outbound.persistence

import com.petcampus.knockdog.global.persistence.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class UserJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
    @Column(name = "user_code", nullable = false, unique = true, length = 8)
    val userCode: String,
    @Column(name = "nickname", length = 100)
    val nickname: String? = null,
    @Column(name = "profile_image", length = 500)
    val profileImage: String? = null,
    @Column(name = "info_receive_email")
    val infoReceiveEmail: String? = null,
    @Column(name = "gender", length = 20)
    val gender: String? = null,
    @Column(name = "phone_number", length = 20)
    val phoneNumber: String? = null,
    @Column(name = "emergency_phone_number", length = 20)
    val emergencyPhoneNumber: String? = null,
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val addresses: MutableList<UserAddressJpaEntity> = mutableListOf(),
    deletedAt: LocalDateTime? = null,
) : BaseEntity(deletedAt)
