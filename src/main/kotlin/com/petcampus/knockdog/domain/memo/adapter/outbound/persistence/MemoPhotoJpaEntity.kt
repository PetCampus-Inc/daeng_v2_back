package com.petcampus.knockdog.domain.memo.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "memo_photos")
class MemoPhotoJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
    @Column(name = "memo_id", nullable = false)
    val memoId: Long,
    @Column(name = "object_key", nullable = false, length = 512)
    val objectKey: String,
    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int,
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
