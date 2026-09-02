package com.petcampus.knockdog.domain.kindergarten.adapter.inbound.web

import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenDistanceCalculator
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenOperatingStatusCalculator
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * `GET /api/v0/kindergarten/main/{id}` 응답 — 레거시 `MainInfoResponseDto`와 계약을 맞춘다.
 *
 * 알려진 계약 차이(레거시 버그를 그대로 이식한 것 포함, 로컬 응답 대조 시 확인 대상):
 * - `roadAddress` 필드는 이름과 달리 실제로 `address`(지번 주소) 값을 담는다 — 레거시
 *   `KindergartenMapper.toMainInfoResponseDto`가 `dto.getAddress()`를 그대로 넣는 걸 그대로 이식했다.
 * - `operationStatus`는 `OperatingStatusDescription.title`이 "영업중"일 때만 OPEN이고, 그 외(휴무 포함)는
 *   전부 CLOSED다 — 레거시 `OperationStatusParser.parseOperationInfo`가 HOLIDAY를 구분하지 않는 걸 그대로 이식했다.
 * - `bookmarked`/`memoData`는 `bookmark`/`memo` 도메인이 없어 고정값이다(docs/work/KD3-413-kindergarten-static-lookup.md).
 * - `serviceTags`는 4개 옵션 그룹의 코드를 합친 것이고, 레거시의 OPEN_NOW/가격정책 파생 태그는 포함하지 않는다.
 */
data class KindergartenSummaryResponse(
    val id: String,
    val title: String,
    val ctg: String,
    val operationTimes: OperationTimes,
    val operationStatus: String,
    val businessStatus: BusinessStatusResponse,
    val schoolStatus: String,
    val price: Int,
    val dist: Double,
    val roadAddress: String,
    val reviewCount: Int,
    val serviceTags: List<String>,
    val banner: List<String>,
    val bookmarked: Boolean,
    val phoneNumber: String?,
    val coords: Coords,
    val memoData: Any?,
) {
    data class OperationTimes(
        val startTime: String?,
        val endTime: String?,
    )

    data class BusinessStatusResponse(
        val title: String,
        val description: String,
    )

    data class Coords(
        val lng: Double?,
        val lat: Double?,
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
                title = kindergarten.name,
                ctg = kindergarten.categories.joinToString(",") { it.value },
                operationTimes =
                    OperationTimes(
                        startTime = range?.start?.let { TIME_FMT.format(it) },
                        endTime = range?.end?.let { TIME_FMT.format(it) },
                    ),
                operationStatus = if (status.title == "영업중") "OPEN" else "CLOSED",
                businessStatus = BusinessStatusResponse(status.title, status.description),
                schoolStatus = kindergarten.status.name,
                price = kindergarten.lowestPrice,
                dist = dist,
                roadAddress = kindergarten.address,
                reviewCount = kindergarten.reviewCount,
                serviceTags = KindergartenServiceTags.allOf(kindergarten),
                banner = KindergartenServiceTags.banner(kindergarten),
                bookmarked = false,
                phoneNumber = kindergarten.phoneNumber,
                coords = Coords(lng = kindergarten.longitude, lat = kindergarten.latitude),
                memoData = null,
            )
        }
    }
}
