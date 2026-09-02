package com.petcampus.knockdog.domain.kindergarten.adapter.inbound.web

import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenDistanceCalculator
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenOperatingStatusCalculator
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class KindergartenSummaryResponse(
    val id: String,
    val name: String,
    val categories: List<String>,
    val address: String,
    val addressDetail: String?,
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
                addressDetail = kindergarten.addressDetail,
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

        private fun operationStatusOf(title: String): String =
            when (title) {
                "영업중" -> "OPEN"
                "오늘 휴무" -> "HOLIDAY"
                else -> "CLOSED"
            }
    }
}
