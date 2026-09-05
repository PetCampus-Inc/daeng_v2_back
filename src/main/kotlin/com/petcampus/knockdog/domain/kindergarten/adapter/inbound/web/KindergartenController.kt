package com.petcampus.knockdog.domain.kindergarten.adapter.inbound.web

import com.petcampus.knockdog.domain.kindergarten.application.port.input.GetKindergartenUseCase
import com.petcampus.knockdog.global.response.Response
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/kindergartens")
class KindergartenController(
    private val getKindergartenUseCase: GetKindergartenUseCase,
) {
    @GetMapping("/{id}/summary")
    fun getSummary(
        @PathVariable id: String,
        @RequestParam lat: Double,
        @RequestParam lng: Double,
    ): Response<KindergartenSummaryResponse> {
        val kindergarten = getKindergartenUseCase.getByNaverPlaceId(id)
        return Response.success(KindergartenSummaryResponse.from(kindergarten, lat, lng))
    }

    @GetMapping("/{id}/detail")
    fun getDetail(
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
