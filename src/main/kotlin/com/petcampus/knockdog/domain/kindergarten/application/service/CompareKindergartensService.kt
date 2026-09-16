package com.petcampus.knockdog.domain.kindergarten.application.service

import com.petcampus.knockdog.domain.kindergarten.application.KindergartenErrorCode
import com.petcampus.knockdog.domain.kindergarten.application.port.input.CompareKindergartensCommand
import com.petcampus.knockdog.domain.kindergarten.application.port.input.CompareKindergartensResult
import com.petcampus.knockdog.domain.kindergarten.application.port.input.CompareKindergartensUseCase
import com.petcampus.knockdog.domain.kindergarten.application.port.output.LoadComparisonAddressesPort
import com.petcampus.knockdog.domain.kindergarten.application.port.output.LoadKindergartenPort
import com.petcampus.knockdog.domain.kindergarten.application.port.output.LoadTravelTimesPort
import com.petcampus.knockdog.domain.kindergarten.domain.ComparisonReferencePoint
import com.petcampus.knockdog.domain.kindergarten.domain.ComparisonReferencePointType
import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten
import com.petcampus.knockdog.domain.kindergarten.domain.TravelTime
import com.petcampus.knockdog.global.exception.BusinessException
import com.petcampus.knockdog.global.exception.CommonErrorCode
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

@Service
class CompareKindergartensService(
    private val loadKindergartenPort: LoadKindergartenPort,
    private val loadComparisonAddressesPort: LoadComparisonAddressesPort,
    private val loadTravelTimesPort: LoadTravelTimesPort,
    @Qualifier("travelTimePairExecutor") private val pairExecutor: Executor,
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
            travelTimesByKindergarten = travelTimesByKindergarten(kindergartens, referencePoints),
        )
    }

    private fun travelTimesByKindergarten(
        kindergartens: List<Kindergarten>,
        referencePoints: List<ComparisonReferencePoint>,
    ): Map<String, List<List<TravelTime>>> {
        val pendingTravelTimesByKindergarten = launchTravelTimeFetches(kindergartens, referencePoints)

        return kindergartens.associate { kindergarten ->
            val pending = pendingTravelTimesByKindergarten.getValue(kindergarten)
            kindergarten.naverPlaceId to (pending?.map { it.join() } ?: referencePoints.map { emptyList() })
        }
    }

    private fun launchTravelTimeFetches(
        kindergartens: List<Kindergarten>,
        referencePoints: List<ComparisonReferencePoint>,
    ): Map<Kindergarten, List<CompletableFuture<List<TravelTime>>>?> =
        kindergartens.associateWith { kindergarten ->
            val lat = kindergarten.lat
            val lng = kindergarten.lng
            if (lat == null || lng == null) {
                null
            } else {
                referencePoints.map { point ->
                    CompletableFuture.supplyAsync(
                        { loadTravelTimesPort.findTravelTimes(point.lat, point.lng, lat, lng) },
                        pairExecutor,
                    )
                }
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
