package com.petcampus.knockdog.domain.kindergarten.adapter.outbound.seed

data class CrawledKindergarten(
    val id: Long,
    val name: String,
    val categories: List<String> = emptyList(),
    val tel: String? = null,
    val thumbnailS3Key: String? = null,
    val menuImageS3Keys: List<String> = emptyList(),
    val roadAddress: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val links: List<CrawledLink> = emptyList(),
    val dogBreedsAccepted: List<String> = emptyList(),
    val dogServices: List<String> = emptyList(),
    val dogSafetyFacilities: List<String> = emptyList(),
    val visitorAmenities: List<String> = emptyList(),
    val businessHours: List<CrawledBusinessHour> = emptyList(),
    val reviewCount: CrawledReviewCount? = null,
)

data class CrawledLink(
    val code: String,
    val url: String,
)

data class CrawledBusinessHour(
    val name: String,
    val weekdays: CrawledTimeRange? = null,
    val weekends: CrawledTimeRange? = null,
    val offdays: List<String> = emptyList(),
)

data class CrawledTimeRange(
    val open: String? = null,
    val close: String? = null,
)

data class CrawledReviewCount(
    val blogReviewCount: Int = 0,
    val visitReviewCount: Int = 0,
)

data class CrawledMenu(
    val kindergartenId: Long,
    val productType: String,
    val serviceType: String,
    val productName: String,
    val unitStr: String? = null,
    val unit: Double? = null,
    val unitType: String? = null,
    val weightRange: String? = null,
    val price: Double? = null,
    val hourlyPrice: Double? = null,
    val minPrice: Boolean = false,
    val maxPrice: Boolean = false,
    val totalDurationStr: String? = null,
    val totalDurationMinutes: Int? = null,
)
