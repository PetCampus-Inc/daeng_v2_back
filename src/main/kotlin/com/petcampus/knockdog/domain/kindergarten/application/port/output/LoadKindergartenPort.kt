package com.petcampus.knockdog.domain.kindergarten.application.port.output

import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten

interface LoadKindergartenPort {
    fun findByNaverPlaceId(naverPlaceId: String): Kindergarten?

    fun findByNaverPlaceIds(naverPlaceIds: List<String>): List<Kindergarten>

    fun findCardSummariesByNaverPlaceIds(naverPlaceIds: List<String>): List<KindergartenCardSummary> =
        findByNaverPlaceIds(naverPlaceIds).map { kindergarten ->
            KindergartenCardSummary(
                id = kindergarten.naverPlaceId,
                name = kindergarten.name,
                thumbnailS3Key = kindergarten.thumbnailS3Key,
                categories = kindergarten.categories.map { it.value },
                address = kindergarten.address,
                lowestPrice = kindergarten.lowestPrice,
                blogReviewCount = kindergarten.blogReviewCount,
                lat = kindergarten.lat,
                lng = kindergarten.lng,
                closed = kindergarten.status.name == CLOSED_STATUS,
            )
        }

    companion object {
        private const val CLOSED_STATUS = "CLOSED"
    }
}
