package com.petcampus.knockdog.domain.auth.adapter.outbound.persistence

import com.petcampus.knockdog.domain.auth.domain.AgreementTermType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * user_agreements.user_id: 연관관계 매핑 없이 식별자만 들고 있다. 동의 이력은 User와 수명주기를 공유하지 않으며
 * (탈퇴해도 남길 수 있어야 함) DB FK 제약도 걸지 않는다 (docs/conventions/jpa-entity.md).
 * BaseEntity를 상속하지 않는 이유: 동의는 갱신·soft delete 대상이 아니라 append-only 이력이다.
 */
@Entity
@Table(name = "user_agreements")
class UserAgreementJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "term_type", nullable = false, length = 30)
    val termType: AgreementTermType,
    @Column(name = "agreed_at", nullable = false)
    val agreedAt: LocalDateTime,
)
