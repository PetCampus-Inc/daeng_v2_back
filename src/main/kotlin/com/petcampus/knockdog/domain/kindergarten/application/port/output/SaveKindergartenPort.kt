package com.petcampus.knockdog.domain.kindergarten.application.port.output

import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten

/** JSON 시딩 전용 포트 — 조회 API는 이 포트를 쓰지 않는다. */
interface SaveKindergartenPort {
    fun save(kindergarten: Kindergarten): Kindergarten

    fun existsByNaverPlaceId(naverPlaceId: String): Boolean

    fun count(): Long
}
