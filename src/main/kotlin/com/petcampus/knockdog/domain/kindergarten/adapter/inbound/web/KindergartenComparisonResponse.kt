package com.petcampus.knockdog.domain.kindergarten.adapter.inbound.web

import com.petcampus.knockdog.domain.kindergarten.domain.ComparisonReferencePoint
import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenBusinessHour
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenDistanceCalculator
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenPricingComparison
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenPricingComparisonCalculator
import java.time.LocalTime
import java.util.Locale
import com.petcampus.knockdog.domain.kindergarten.domain.TravelTime as DomainTravelTime

data class KindergartenComparisonResponse(
    val id: String,
    val name: String,
    val thumbnailS3Key: String?,
    val categories: List<String>,
    val pricing: Pricing?,
    val service: List<String>,
    val distance: List<Distance>,
    val operatingSchedule: OperatingSchedule?,
) {
    data class Pricing(
        val countHourlyAvg: Int,
        val monthlyHourlyAvg: Int,
        val products: List<Product>,
    ) {
        data class Product(
            val serviceType: String,
            val min: PriceItem,
            val max: PriceItem,
            val countTicketAvg: Int,
            val monthlyHourlyAvg: Int,
        )

        data class PriceItem(
            val name: String,
            val price: Int,
        )
    }

    data class Distance(
        val referencePoint: String,
        val distance: String,
        val travelTimes: List<TravelTime>,
    )

    data class TravelTime(
        val type: String,
        val time: Int?,
    )

    data class OperatingSchedule(
        val weekday: TimeRange?,
        val weekend: TimeRange?,
        val closedDays: List<String>,
    )

    data class TimeRange(
        val open: LocalTime,
        val close: LocalTime,
    )

    companion object {
        private const val DEFAULT_PROFILE_NAME = "DEFAULT"

        fun from(
            kindergarten: Kindergarten,
            referencePoints: List<ComparisonReferencePoint>,
            travelTimes: List<List<DomainTravelTime>> = referencePoints.map { emptyList() },
        ): KindergartenComparisonResponse =
            KindergartenComparisonResponse(
                id = kindergarten.naverPlaceId,
                name = kindergarten.name,
                thumbnailS3Key = kindergarten.thumbnailS3Key,
                categories = kindergarten.categories.map { it.value },
                pricing = pricingOf(kindergarten),
                service = KindergartenServiceTags.allOf(kindergarten),
                distance = distancesOf(kindergarten, referencePoints, travelTimes),
                operatingSchedule = operatingScheduleOf(kindergarten),
            )

        private fun pricingOf(kindergarten: Kindergarten): Pricing? {
            val comparison = KindergartenPricingComparisonCalculator.calculate(kindergarten.menus) ?: return null
            return Pricing(
                countHourlyAvg = comparison.countHourlyAvg,
                monthlyHourlyAvg = comparison.monthlyHourlyAvg,
                products = comparison.products.map { it.toResponse() },
            )
        }

        private fun KindergartenPricingComparison.Product.toResponse(): Pricing.Product =
            Pricing.Product(
                serviceType = serviceType,
                min = Pricing.PriceItem(min.name, min.price),
                max = Pricing.PriceItem(max.name, max.price),
                countTicketAvg = countTicketAvg,
                monthlyHourlyAvg = monthlyHourlyAvg,
            )

        private fun distancesOf(
            kindergarten: Kindergarten,
            referencePoints: List<ComparisonReferencePoint>,
            travelTimes: List<List<DomainTravelTime>>,
        ): List<Distance> {
            val lat = kindergarten.lat ?: return emptyList()
            val lng = kindergarten.lng ?: return emptyList()
            return referencePoints.mapIndexed { index, point ->
                Distance(
                    referencePoint = point.type.name,
                    distance =
                        String.format(
                            Locale.KOREA,
                            "%.1fkm",
                            KindergartenDistanceCalculator.calculateKm(point.lat, point.lng, lat, lng),
                        ),
                    travelTimes = travelTimes.getOrElse(index) { emptyList() }.map { it.toResponse() },
                )
            }
        }

        private fun DomainTravelTime.toResponse(): TravelTime = TravelTime(type = type.name, time = seconds)

        private fun operatingScheduleOf(kindergarten: Kindergarten): OperatingSchedule? {
            val profile = selectProfile(kindergarten.businessHours) ?: return null
            return OperatingSchedule(
                weekday = rangeOf(profile.weekdayOpen, profile.weekdayClose),
                weekend = rangeOf(profile.weekendOpen, profile.weekendClose),
                closedDays = profile.offdays.map { it.name },
            )
        }

        private fun selectProfile(businessHours: List<KindergartenBusinessHour>): KindergartenBusinessHour? =
            businessHours.firstOrNull { it.name == DEFAULT_PROFILE_NAME } ?: businessHours.firstOrNull()

        private fun rangeOf(
            open: LocalTime?,
            close: LocalTime?,
        ): TimeRange? = if (open != null && close != null) TimeRange(open, close) else null
    }
}
