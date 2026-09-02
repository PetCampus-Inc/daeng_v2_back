package com.petcampus.knockdog.domain.kindergarten.application.port.input

import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten

/**
 * 정적 조회 3종(요약/상세/요금표)이 공통으로 쓰는 단일 조회 유스케이스.
 * 응답 모양 차이는 애플리케이션 로직이 아니라 웹 계층의 DTO 매핑 문제라 유스케이스를 나누지 않는다.
 */
interface GetKindergartenUseCase {
    /**
     * `naverPlaceId` 기준 조회 — v0 경로 `{id}`가 실제로 받는 값이다(내부 PK가 아니다).
     * 존재하지 않으면 [com.petcampus.knockdog.global.exception.BusinessException]을 던진다.
     */
    fun getByNaverPlaceId(naverPlaceId: String): Kindergarten
}
