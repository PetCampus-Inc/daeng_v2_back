package com.petcampus.knockdog.domain.auth.adapter.outbound.persistence

import com.petcampus.knockdog.domain.auth.domain.Provider
import com.petcampus.knockdog.domain.auth.domain.SocialUserStatus
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

/** email에는 UNIQUE 제약을 걸지 않는다 — 동일 이메일 다중 provider row를 허용해야 한다(레거시 VerifyOidcService 로직 포팅). */
@Entity
@Table(name = "social_users")
class SocialUserJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    val provider: Provider,
    @Column(name = "provider_id", nullable = false)
    val providerId: String,
    @Column(name = "email", nullable = false)
    val email: String,
    @Column(name = "name")
    val name: String? = null,
    @Column(name = "picture", length = 500)
    val picture: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: SocialUserStatus,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val user: UserJpaEntity? = null,
    @Column(name = "linked_at")
    val linkedAt: LocalDateTime? = null,
    deletedAt: LocalDateTime? = null,
) : BaseEntity(deletedAt)
