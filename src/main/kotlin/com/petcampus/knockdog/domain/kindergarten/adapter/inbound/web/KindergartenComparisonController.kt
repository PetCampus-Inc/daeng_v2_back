package com.petcampus.knockdog.domain.kindergarten.adapter.inbound.web

import com.petcampus.knockdog.domain.kindergarten.application.port.input.CompareKindergartensCommand
import com.petcampus.knockdog.domain.kindergarten.application.port.input.CompareKindergartensUseCase
import com.petcampus.knockdog.global.response.Response
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/kindergartens")
class KindergartenComparisonController(
    private val compareKindergartensUseCase: CompareKindergartensUseCase,
) {
    @GetMapping("/comparisons")
    fun compare(
        @RequestParam ids: List<String>,
        @RequestParam(required = false) lat: Double?,
        @RequestParam(required = false) lng: Double?,
        @AuthenticationPrincipal principal: String?,
    ): Response<List<KindergartenComparisonResponse>> {
        val result =
            compareKindergartensUseCase.compare(
                CompareKindergartensCommand(
                    naverPlaceIds = ids,
                    userCode = principal?.takeUnless { it == ANONYMOUS_PRINCIPAL },
                    lat = lat,
                    lng = lng,
                ),
            )

        return Response.success(
            result.kindergartens.map { KindergartenComparisonResponse.from(it, result.referencePoints) },
        )
    }

    companion object {
        private const val ANONYMOUS_PRINCIPAL = "anonymousUser"
    }
}
