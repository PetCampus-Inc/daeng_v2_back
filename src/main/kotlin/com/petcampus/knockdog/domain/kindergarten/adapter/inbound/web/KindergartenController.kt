package com.petcampus.knockdog.domain.kindergarten.adapter.inbound.web

import com.petcampus.knockdog.domain.kindergarten.application.port.input.GetKindergartenUseCase
import com.petcampus.knockdog.global.response.Response
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * `v0` 계약 보존 대상(KEEP) — 레거시 `KindergartenController`의 정적 조회 3개 엔드포인트만 이관한다.
 * map-view/near/autocomplete/filters 등 지도·좌표 기반 동적 조회는 후속 하위 작업이다
 * (docs/work/KD3-413-kindergarten-static-lookup.md).
 */
@RestController
@RequestMapping("/api/v0/kindergarten")
class KindergartenController(
    private val getKindergartenUseCase: GetKindergartenUseCase,
) {
    @GetMapping("/main/{id}")
    fun getMain(
        @PathVariable id: String,
        @RequestParam lat: Double,
        @RequestParam lng: Double,
    ): Response<KindergartenSummaryResponse> {
        val kindergarten = getKindergartenUseCase.getByNaverPlaceId(id)
        return Response.success(KindergartenSummaryResponse.from(kindergarten, lat, lng))
    }

    @GetMapping("/basic/{id}")
    fun getBasic(
        @PathVariable id: String,
    ): Response<KindergartenDetailResponse> {
        val kindergarten = getKindergartenUseCase.getByNaverPlaceId(id)
        return Response.success(KindergartenDetailResponse.from(kindergarten))
    }

    @GetMapping("/{id}/pricing")
    fun getPricing(
        @PathVariable id: String,
    ): Response<KindergartenPricingResponse> {
        val kindergarten = getKindergartenUseCase.getByNaverPlaceId(id)
        return Response.success(KindergartenPricingResponse.from(kindergarten))
    }
}
