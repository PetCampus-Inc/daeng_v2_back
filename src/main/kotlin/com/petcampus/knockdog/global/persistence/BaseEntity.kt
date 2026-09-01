package com.petcampus.knockdog.global.persistence

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * 모든 JPA 엔티티가 공통 상속하는 created_at/updated_at/deleted_at.
 * BaseEntity로 표현 가능한 상태(soft-delete)는 별도 status 컬럼을 두지 않는다 (docs/conventions/jpa-entity.md).
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity(
    @Column(name = "deleted_at")
    val deletedAt: LocalDateTime? = null,
) {
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
}
