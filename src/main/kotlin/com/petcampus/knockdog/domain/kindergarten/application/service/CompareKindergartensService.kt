package com.petcampus.knockdog.domain.kindergarten.application.service

import com.petcampus.knockdog.domain.kindergarten.application.KindergartenErrorCode
import com.petcampus.knockdog.domain.kindergarten.application.port.input.CompareKindergartensCommand
import com.petcampus.knockdog.domain.kindergarten.application.port.input.CompareKindergartensResult
import com.petcampus.knockdog.domain.kindergarten.application.port.input.CompareKindergartensUseCase
import com.petcampus.knockdog.domain.kindergarten.application.port.output.LoadComparisonAddressesPort
import com.petcampus.knockdog.domain.kindergarten.application.port.output.LoadKindergartenPort
import com.petcampus.knockdog.domain.kindergarten.application.port.output.LoadTransitTimesPort
import com.petcampus.knockdog.domain.kindergarten.domain.ComparisonReferencePoint
import com.petcampus.knockdog.domain.kindergarten.domain.ComparisonReferencePointType
import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten
import com.petcampus.knockdog.domain.kindergarten.domain.TransitTime
import com.petcampus.knockdog.global.exception.BusinessException
import com.petcampus.knockdog.global.exception.CommonErrorCode
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class CompareKindergartensService(
    private val loadKindergartenPort: LoadKindergartenPort,
    private val loadComparisonAddressesPort: LoadComparisonAddressesPort,
    private val loadTransitTimesPort: LoadTransitTimesPort,
) : CompareKindergartensUseCase {
    override fun compare(command: CompareKindergartensCommand): CompareKindergartensResult {
        val naverPlaceIds = command.naverPlaceIds
        if (naverPlaceIds.size != COMPARISON_TARGET_COUNT) {
            throw BusinessException(KindergartenErrorCode.COMPARISON_TARGET_COUNT)
        }
        if (naverPlaceIds.toSet().size != naverPlaceIds.size) {
            throw BusinessException(KindergartenErrorCode.COMPARISON_TARGET_DUPLICATED)
        }

        val loaded = loadKindergartenPort.findByNaverPlaceIds(naverPlaceIds).associateBy { it.naverPlaceId }
        if (loaded.size != naverPlaceIds.size) {
            throw BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND)
        }

        val kindergartens = naverPlaceIds.map { loaded.getValue(it) }
        val referencePoints = resolveReferencePoints(command)

        return CompareKindergartensResult(
            kindergartens = kindergartens,
            referencePoints = referencePoints,
            transitTimesByKindergarten = transitTimesByKindergarten(kindergartens, referencePoints),
        )
    }

    private fun transitTimesByKindergarten(
        kindergartens: List<Kindergarten>,
        referencePoints: List<ComparisonReferencePoint>,
    ): Map<String, List<List<TransitTime>>> {
        // 유치원 × 기준점 쌍마다 외부 API를 호출하므로 쌍 사이에도 병렬로 조회한다
        // (쌍 안의 도보·자동차·대중교통 3종은 어댑터가 이미 병렬로 조회한다).
        val futuresByKindergarten =
            kindergartens.associateWith { kindergarten ->
                val lat = kindergarten.lat
                val lng = kindergarten.lng
                if (lat == null || lng == null) {
                    null
                } else {
                    referencePoints.map { point ->
                        CompletableFuture.supplyAsync { loadTransitTimesPort.findTransitTimes(point.lat, point.lng, lat, lng) }
                    }
                }
            }

        return kindergartens.associate { kindergarten ->
            val futures = futuresByKindergarten.getValue(kindergarten)
            kindergarten.naverPlaceId to (futures?.map { it.join() } ?: referencePoints.map { emptyList() })
        }
    }

    private fun resolveReferencePoints(command: CompareKindergartensCommand): List<ComparisonReferencePoint> {
        if (command.lat != null && command.lng != null) {
            return listOf(ComparisonReferencePoint(ComparisonReferencePointType.OTHER, command.lat, command.lng))
        }
        if (command.userCode != null) {
            return loadComparisonAddressesPort
                .findByUserCode(command.userCode)
                .sortedBy { it.type != ComparisonReferencePointType.HOME }
        }
        return emptyList()
    }

    companion object {
        private const val COMPARISON_TARGET_COUNT = 2
    }
}
