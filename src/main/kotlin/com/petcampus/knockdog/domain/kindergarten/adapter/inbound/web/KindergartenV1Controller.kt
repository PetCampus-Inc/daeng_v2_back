package com.petcampus.knockdog.domain.kindergarten.adapter.inbound.web

import com.petcampus.knockdog.domain.kindergarten.application.port.input.GetKindergartenUseCase
import com.petcampus.knockdog.global.response.Response
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * `v0`(`main`/`basic`/`pricing`, [KindergartenController])의 재설계판. `v0`에서 발견한 버그 중
 * `roadAddress` 오염, `operationStatus` HOLIDAY 미구분, `breakTime` 오염을 고쳤다
 * (docs/domains/kindergarten.md §2).
 *
 * `v0`를 이 서버에서 계속 host할지, 레거시로만 넘길지는 프론트 확인 후 결정 — 그 결정과 무관하게
 * `v1`은 지금 추가한다.
 */
@RestController
@RequestMapping("/api/v1/kindergartens")
class KindergartenV1Controller(
    private val getKindergartenUseCase: GetKindergartenUseCase,
) {
    @GetMapping("/{id}/summary")
    fun getSummary(
        @PathVariable id: String,
        @RequestParam lat: Double,
        @RequestParam lng: Double,
    ): Response<KindergartenSummaryV1Response> {
        val kindergarten = getKindergartenUseCase.getByNaverPlaceId(id)
        return Response.success(KindergartenSummaryV1Response.from(kindergarten, lat, lng))
    }

    @GetMapping("/{id}/detail")
    fun getDetail(
        @PathVariable id: String,
    ): Response<KindergartenDetailV1Response> {
        val kindergarten = getKindergartenUseCase.getByNaverPlaceId(id)
        return Response.success(KindergartenDetailV1Response.from(kindergarten))
    }

    @GetMapping("/{id}/pricing")
    fun getPricing(
        @PathVariable id: String,
    ): Response<KindergartenPricingResponse> {
        val kindergarten = getKindergartenUseCase.getByNaverPlaceId(id)
        return Response.success(KindergartenPricingResponse.from(kindergarten))
    }
}
