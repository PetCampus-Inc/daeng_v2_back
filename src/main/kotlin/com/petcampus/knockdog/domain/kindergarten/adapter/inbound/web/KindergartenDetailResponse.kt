package com.petcampus.knockdog.domain.kindergarten.adapter.inbound.web

import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenOptionGroup

/**
 * `GET /api/v0/kindergarten/basic/{id}` 응답 — 레거시 `BasicInfoResponseDto`와 계약을 맞춘다.
 *
 * 알려진 계약 차이:
 * - `roadAddress`는 `main/{id}`과 같은 이유로 실제로는 `address` 값이다(레거시 그대로 이식).
 * - `operationTimes[].weekday`/`weekend`의 `breakTime` 필드는 레거시에서 실제로는 마감 시각을 담는
 *   버그로 보인다(크롤링 데이터에 브레이크타임 개념 자체가 없다) — 그대로 이식했다.
 * - `lastUpdatedAt`은 크롤링 데이터에 원본 타임스탬프가 없어 고정 문구를 반환한다. 원장 편집 이력이
 *   생기면 그때 실제 값으로 교체한다(이번 범위 밖).
 */
data class KindergartenDetailResponse(
    val id: String,
    val roadAddress: String,
    val coord: Coord,
    val operationTimes: List<BusinessHoursResponse>,
    val dogBreeds: List<String>,
    val dogServices: List<String>,
    val dogSafetyFacilities: List<String>,
    val visitorAmenities: List<String>,
    val homepageUrl: String?,
    val instagramUrl: String?,
    val youtubeUrl: String?,
    val lastUpdatedAt: String,
    val schoolStatus: String,
) {
    data class Coord(
        val lat: Double?,
        val lng: Double?,
    )

    data class BusinessHoursResponse(
        val serviceTags: String,
        val weekday: List<TimeWithBreak>,
        val weekend: List<TimeWithBreak>,
        val closedDays: List<String>,
    )

    data class TimeWithBreak(
        val time: String?,
        val breakTime: String?,
    )

    companion object {
        private const val DEFAULT_LAST_UPDATED_AT_DISPLAY = "정보 없음"

        fun from(kindergarten: Kindergarten): KindergartenDetailResponse =
            KindergartenDetailResponse(
                id = requireNotNull(kindergarten.naverPlaceId),
                roadAddress = kindergarten.address,
                coord = Coord(lat = kindergarten.latitude, lng = kindergarten.longitude),
                operationTimes = kindergarten.businessHours.mapNotNull { it.toResponseOrNull() },
                dogBreeds = KindergartenServiceTags.group(kindergarten, KindergartenOptionGroup.DOG_BREED),
                dogServices = KindergartenServiceTags.group(kindergarten, KindergartenOptionGroup.DOG_SERVICE),
                dogSafetyFacilities = KindergartenServiceTags.group(kindergarten, KindergartenOptionGroup.SAFETY_FACILITY),
                visitorAmenities = KindergartenServiceTags.group(kindergarten, KindergartenOptionGroup.VISITOR_AMENITY),
                homepageUrl = kindergarten.linkOf("HOMEPAGE"),
                instagramUrl = kindergarten.linkOf("INSTAGRAM"),
                youtubeUrl = kindergarten.linkOf("YOUTUBE"),
                lastUpdatedAt = DEFAULT_LAST_UPDATED_AT_DISPLAY,
                schoolStatus = kindergarten.status.name,
            )

        private fun com.petcampus.knockdog.domain.kindergarten.domain.KindergartenBusinessHour.toResponseOrNull(): BusinessHoursResponse? {
            val weekday = weekdayOpen?.let { open -> weekdayClose?.let { close -> TimeWithBreak(fmt(open), fmt(close)) } }
            val weekend = weekendOpen?.let { open -> weekendClose?.let { close -> TimeWithBreak(fmt(open), fmt(close)) } }
            val closedDays = offdays.map { it.name }
            if (weekday == null && weekend == null && closedDays.isEmpty()) return null

            return BusinessHoursResponse(
                serviceTags = name,
                weekday = listOfNotNull(weekday),
                weekend = listOfNotNull(weekend),
                closedDays = closedDays,
            )
        }

        private fun fmt(time: java.time.LocalTime): String =
            time.format(
                java.time.format.DateTimeFormatter
                    .ofPattern("HH:mm"),
            )
    }
}
