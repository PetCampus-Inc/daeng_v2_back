package com.petcampus.knockdog.domain.kindergarten.adapter.inbound.web

import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenDistanceCalculator
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenOperatingStatusCalculator
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * `GET /api/v1/kindergartens/{id}/summary` 응답 — 레거시(`daeng_v1_back`) `main/{id}`의 재설계판.
 * 발견한 레거시 버그 중 이 응답에 있던 2개를 고쳤다(docs/domains/kindergarten.md §2):
 * - `address`/`roadAddress`를 분리해서 각각 실제 값을 담는다(레거시는 `roadAddress`에 `address` 값이 들어감).
 * - `operationStatus`에 `HOLIDAY`를 구분해서 넣는다(레거시는 휴무도 `CLOSED`로 뭉뚱그림).
 *
 * `bookmarked`/`memoData`는 아예 넣지 않는다 — `bookmark`/`memo` 도메인이 생기면 필드를 추가하는 쪽으로 간다.
 */
data class KindergartenSummaryResponse(
    val id: String,
    val name: String,
    val categories: List<String>,
    val address: String,
    val roadAddress: String?,
    val operationTimes: OperationTimes,
    val operationStatus: String,
    val businessStatusDescription: String,
    val status: String,
    val lowestPrice: Int,
    val distanceKm: Double,
    val reviewCount: Int,
    val serviceTags: List<String>,
    val banner: List<String>,
    val phoneNumber: String?,
    val lat: Double?,
    val lng: Double?,
) {
    data class OperationTimes(
        val startTime: String?,
        val endTime: String?,
    )

    companion object {
        private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")

        fun from(
            kindergarten: Kindergarten,
            userLat: Double,
            userLng: Double,
            now: LocalDateTime = LocalDateTime.now(),
        ): KindergartenSummaryResponse {
            val status = KindergartenOperatingStatusCalculator.calculate(kindergarten.businessHours, now)
            val range = KindergartenOperatingStatusCalculator.todayRange(kindergarten.businessHours, now.toLocalDate())
            val dist =
                if (kindergarten.latitude != null && kindergarten.longitude != null) {
                    KindergartenDistanceCalculator.calculateKm(userLat, userLng, kindergarten.latitude, kindergarten.longitude)
                } else {
                    0.0
                }

            return KindergartenSummaryResponse(
                id = requireNotNull(kindergarten.naverPlaceId),
                name = kindergarten.name,
                categories = kindergarten.categories.map { it.value },
                address = kindergarten.address,
                roadAddress = kindergarten.roadAddress,
                operationTimes =
                    OperationTimes(
                        startTime = range?.start?.let { TIME_FMT.format(it) },
                        endTime = range?.end?.let { TIME_FMT.format(it) },
                    ),
                operationStatus = operationStatusOf(status.title),
                businessStatusDescription = status.description,
                status = kindergarten.status.name,
                lowestPrice = kindergarten.lowestPrice,
                distanceKm = dist,
                reviewCount = kindergarten.reviewCount,
                serviceTags = KindergartenServiceTags.allOf(kindergarten),
                banner = KindergartenServiceTags.banner(kindergarten),
                phoneNumber = kindergarten.phoneNumber,
                lat = kindergarten.latitude,
                lng = kindergarten.longitude,
            )
        }

        /** 레거시와 달리 휴무를 `CLOSED`에 뭉개지 않고 `HOLIDAY`로 구분한다. */
        private fun operationStatusOf(title: String): String =
            when (title) {
                "영업중" -> "OPEN"
                "오늘 휴무" -> "HOLIDAY"
                else -> "CLOSED"
            }
    }
}
