package com.petcampus.knockdog.domain.kindergarten.application.port.output

import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten

interface SaveKindergartenPort {
    fun save(kindergarten: Kindergarten): Kindergarten

    fun existsByNaverPlaceId(naverPlaceId: String): Boolean

    fun count(): Long
}
