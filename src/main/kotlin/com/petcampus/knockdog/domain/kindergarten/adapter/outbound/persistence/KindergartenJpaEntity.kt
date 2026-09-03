package com.petcampus.knockdog.domain.kindergarten.adapter.outbound.persistence

import com.petcampus.knockdog.global.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "kindergartens")
class KindergartenJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
    @Column(name = "naver_place_id", unique = true, length = 100, nullable = false)
    val naverPlaceId: String,
    @Column(name = "name", nullable = false)
    val name: String,
    @Column(name = "phone_number")
    val phoneNumber: String?,
    @Column(name = "address", nullable = false)
    val address: String,
    @Column(name = "address_detail", length = 100)
    val addressDetail: String?,
    @Column(name = "lat")
    val lat: Double?,
    @Column(name = "lng")
    val lng: Double?,
    @Column(name = "thumbnail_s3_key", length = 512)
    val thumbnailS3Key: String?,
    @Column(name = "visitor_review_count", nullable = false)
    val visitorReviewCount: Int,
    @Column(name = "blog_review_count", nullable = false)
    val blogReviewCount: Int,
    @Column(name = "source", nullable = false, length = 20)
    val source: String,
    @Column(name = "status", nullable = false, length = 20)
    val status: String,
    deletedAt: LocalDateTime? = null,
) : BaseEntity(deletedAt)
