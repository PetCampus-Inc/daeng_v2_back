package com.petcampus.knockdog.domain.kindergarten.adapter.outbound.seed

import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenBusinessHour
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenCategory
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenLink
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenMenu
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenOption
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenOptionGroup
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenPriceImage
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object KindergartenSeedConverter {
    private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")

    fun toDomain(
        crawled: CrawledKindergarten,
        menus: List<CrawledMenu>,
    ): Kindergarten =
        Kindergarten.seedFromCrawl(
            naverPlaceId = crawled.id.toString(),
            name = crawled.name,
            phoneNumber = crawled.tel?.takeIf { it.isNotBlank() },
            address = crawled.roadAddress,
            addressDetail = null,
            latitude = crawled.lat,
            longitude = crawled.lng,
            thumbnailS3Key = crawled.thumbnailS3Key,
            visitorReviewCount = crawled.reviewCount?.visitReviewCount ?: 0,
            blogReviewCount = crawled.reviewCount?.blogReviewCount ?: 0,
            categories = crawled.categories.map { KindergartenCategory(it) },
            businessHours = crawled.businessHours.map { it.toDomain() },
            links = crawled.links.map { KindergartenLink(it.code, it.url) },
            options = buildOptions(crawled),
            priceImages = crawled.menuImageS3Keys.mapIndexed { index, s3Key -> KindergartenPriceImage(s3Key, index) },
            menus = menus.mapIndexed { index, menu -> menu.toDomain(index) },
        )

    private fun buildOptions(crawled: CrawledKindergarten): List<KindergartenOption> =
        crawled.dogBreedsAccepted.map { KindergartenOption(KindergartenOptionGroup.DOG_BREED, it) } +
            crawled.dogServices.map { KindergartenOption(KindergartenOptionGroup.DOG_SERVICE, it) } +
            crawled.dogSafetyFacilities.map { KindergartenOption(KindergartenOptionGroup.SAFETY_FACILITY, it) } +
            crawled.visitorAmenities.map { KindergartenOption(KindergartenOptionGroup.VISITOR_AMENITY, it) }

    private fun CrawledBusinessHour.toDomain(): KindergartenBusinessHour =
        KindergartenBusinessHour(
            name = name,
            weekdayOpen = weekdays?.open?.toLocalTimeOrNull(),
            weekdayClose = weekdays?.close?.toLocalTimeOrNull(),
            weekendOpen = weekends?.open?.toLocalTimeOrNull(),
            weekendClose = weekends?.close?.toLocalTimeOrNull(),
            offdays = offdays.mapNotNull { it.toDayOfWeekOrNull() },
        )

    private fun CrawledMenu.toDomain(displayOrder: Int): KindergartenMenu =
        KindergartenMenu(
            productType = productType,
            serviceType = serviceType,
            productName = productName,
            unit = unit,
            unitLabel = unitLabel,
            unitType = unitType,
            weightRange = weightRange,
            price = price?.toInt(),
            hourlyPrice = hourlyPrice?.toInt(),
            isMinPrice = minPrice,
            isMaxPrice = maxPrice,
            totalDurationLabel = totalDurationLabel,
            totalDurationMinutes = totalDurationMinutes,
            displayOrder = displayOrder,
        )

    private fun String.toLocalTimeOrNull(): LocalTime? =
        if (isBlank()) null else runCatching { LocalTime.parse(this, TIME_FMT) }.getOrNull()

    private fun String.toDayOfWeekOrNull(): DayOfWeek? = runCatching { DayOfWeek.valueOf(this) }.getOrNull()
}
