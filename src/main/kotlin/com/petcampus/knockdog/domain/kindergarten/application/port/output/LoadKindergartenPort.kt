package com.petcampus.knockdog.domain.kindergarten.application.port.output

import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten

interface LoadKindergartenPort {
    fun findByNaverPlaceId(naverPlaceId: String): Kindergarten?

    fun findByNaverPlaceIds(naverPlaceIds: List<String>): List<Kindergarten>
}
