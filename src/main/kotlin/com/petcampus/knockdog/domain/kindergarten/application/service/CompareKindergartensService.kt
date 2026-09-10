package com.petcampus.knockdog.domain.kindergarten.application.service

import com.petcampus.knockdog.domain.kindergarten.application.KindergartenErrorCode
import com.petcampus.knockdog.domain.kindergarten.application.port.input.CompareKindergartensCommand
import com.petcampus.knockdog.domain.kindergarten.application.port.input.CompareKindergartensResult
import com.petcampus.knockdog.domain.kindergarten.application.port.input.CompareKindergartensUseCase
import com.petcampus.knockdog.domain.kindergarten.application.port.output.LoadComparisonAddressesPort
import com.petcampus.knockdog.domain.kindergarten.application.port.output.LoadKindergartenPort
import com.petcampus.knockdog.domain.kindergarten.domain.ComparisonReferencePoint
import com.petcampus.knockdog.domain.kindergarten.domain.ComparisonReferencePointType
import com.petcampus.knockdog.global.exception.BusinessException
import com.petcampus.knockdog.global.exception.CommonErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CompareKindergartensService(
    private val loadKindergartenPort: LoadKindergartenPort,
    private val loadComparisonAddressesPort: LoadComparisonAddressesPort,
) : CompareKindergartensUseCase {
    @Transactional(readOnly = true)
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

        return CompareKindergartensResult(
            kindergartens = naverPlaceIds.map { loaded.getValue(it) },
            referencePoints = resolveReferencePoints(command),
        )
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
