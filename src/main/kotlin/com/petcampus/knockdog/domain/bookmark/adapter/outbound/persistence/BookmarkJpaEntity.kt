package com.petcampus.knockdog.domain.bookmark.adapter.outbound.persistence

import com.petcampus.knockdog.global.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(name = "bookmarks", uniqueConstraints = [UniqueConstraint(columnNames = ["user_code", "kindergarten_id"])])
class BookmarkJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
    @Column(name = "user_code", nullable = false, length = 8)
    val userCode: String,
    @Column(name = "kindergarten_id", nullable = false, length = 100)
    val kindergartenId: String,
) : BaseEntity()
