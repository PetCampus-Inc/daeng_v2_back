package com.petcampus.knockdog.domain.kindergarten.adapter.inbound.web

import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenBusinessHour
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenOptionGroup
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * `GET /api/v1/kindergartens/{id}/detail` 응답 — 레거시(`daeng_v1_back`) `basic/{id}`의 재설계판.
 *
 * 레거시 버그였던 `breakTime` 필드를 뺐다 — 크롤링 데이터에 브레이크타임 개념 자체가 없어서 레거시도
 * 실제로는 마감 시각을 잘못 채우고 있었다. `address`/`roadAddress`도 [KindergartenSummaryResponse]와
 * 동일하게 분리했다.
 */
data class KindergartenDetailResponse(
    val id: String,
    val address: String,
    val roadAddress: String?,
    val lat: Double?,
    val lng: Double?,
    val businessHours: List<BusinessHours>,
    val dogBreeds: List<String>,
    val dogServices: List<String>,
    val dogSafetyFacilities: List<String>,
    val visitorAmenities: List<String>,
    val homepageUrl: String?,
    val instagramUrl: String?,
    val youtubeUrl: String?,
    val status: String,
) {
    data class BusinessHours(
        val name: String,
        val weekday: TimeRange?,
        val weekend: TimeRange?,
        val closedDays: List<String>,
    )

    data class TimeRange(
        val open: String,
        val close: String,
    )

    companion object {
        fun from(kindergarten: Kindergarten): KindergartenDetailResponse =
            KindergartenDetailResponse(
                id = requireNotNull(kindergarten.naverPlaceId),
                address = kindergarten.address,
                roadAddress = kindergarten.roadAddress,
                lat = kindergarten.latitude,
                lng = kindergarten.longitude,
                businessHours = kindergarten.businessHours.mapNotNull { it.toResponseOrNull() },
                dogBreeds = KindergartenServiceTags.group(kindergarten, KindergartenOptionGroup.DOG_BREED),
                dogServices = KindergartenServiceTags.group(kindergarten, KindergartenOptionGroup.DOG_SERVICE),
                dogSafetyFacilities = KindergartenServiceTags.group(kindergarten, KindergartenOptionGroup.SAFETY_FACILITY),
                visitorAmenities = KindergartenServiceTags.group(kindergarten, KindergartenOptionGroup.VISITOR_AMENITY),
                homepageUrl = kindergarten.linkOf("HOMEPAGE"),
                instagramUrl = kindergarten.linkOf("INSTAGRAM"),
                youtubeUrl = kindergarten.linkOf("YOUTUBE"),
                status = kindergarten.status.name,
            )

        private fun KindergartenBusinessHour.toResponseOrNull(): BusinessHours? {
            val weekday = rangeOf(weekdayOpen, weekdayClose)
            val weekend = rangeOf(weekendOpen, weekendClose)
            val closedDays = offdays.map { it.name }
            if (weekday == null && weekend == null && closedDays.isEmpty()) return null

            return BusinessHours(name = name, weekday = weekday, weekend = weekend, closedDays = closedDays)
        }

        private fun rangeOf(
            open: LocalTime?,
            close: LocalTime?,
        ): TimeRange? {
            if (open == null || close == null) return null
            val fmt = DateTimeFormatter.ofPattern("HH:mm")
            return TimeRange(fmt.format(open), fmt.format(close))
        }
    }
}
