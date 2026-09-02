package com.petcampus.knockdog.domain.kindergarten.application.port.output

import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten

interface LoadKindergartenPort {
    /** 프론트가 호출하는 `{id}` 경로변수는 내부 PK가 아니라 `naverPlaceId`다(원장 자체 등록 유치원은 `manual_` 접두사). */
    fun findByNaverPlaceId(naverPlaceId: String): Kindergarten?
}
