package com.petcampus.knockdog.domain.bookmark.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 실용형: JPA 엔티티가 곧 도메인 모델이다(별도 순수 모델·매퍼 없음).
 * 단순 CRUD라 도메인/영속성 분리의 이점보다 비용이 커서 하나로 합쳤다.
 */
@Entity
@Table(name = "bookmark")
class BookmarkJpaEntity(
    @Id
    @Column(name = "id", length = 36)
    val id: String,
    @Column(name = "owner_id", nullable = false, length = 36)
    val ownerId: String,
    @Column(name = "kindergarten_id", nullable = false, length = 36)
    val kindergartenId: String,
)
