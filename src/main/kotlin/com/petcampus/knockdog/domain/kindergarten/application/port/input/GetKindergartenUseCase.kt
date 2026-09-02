package com.petcampus.knockdog.domain.kindergarten.application.port.input

import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten

interface GetKindergartenUseCase {
    fun getByNaverPlaceId(naverPlaceId: String): Kindergarten
}
