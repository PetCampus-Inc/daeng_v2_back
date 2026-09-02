package com.petcampus.knockdog.domain.kindergarten.domain

class Kindergarten private constructor(
    val id: KindergartenId?,
    val naverPlaceId: String?,
    val name: String,
    val phoneNumber: String?,
    val address: String,
    val addressDetail: String?,
    val latitude: Double?,
    val longitude: Double?,
    val thumbnailS3Key: String?,
    val visitorReviewCount: Int,
    val blogReviewCount: Int,
    val source: KindergartenSource,
    val status: KindergartenStatus,
    val categories: List<KindergartenCategory>,
    val businessHours: List<KindergartenBusinessHour>,
    val links: List<KindergartenLink>,
    val options: List<KindergartenOption>,
    val priceImages: List<KindergartenPriceImage>,
    val menus: List<KindergartenMenu>,
) {
    val reviewCount: Int
        get() = visitorReviewCount.coerceAtLeast(0) + blogReviewCount.coerceAtLeast(0)

    val lowestPrice: Int
        get() = menus.mapNotNull { it.price }.minOrNull() ?: 0

    fun optionsOf(group: KindergartenOptionGroup): List<KindergartenOption> = options.filter { it.group == group }.sortedBy { it.code }

    fun linkOf(code: String): String? = links.firstOrNull { it.code.equals(code, ignoreCase = true) }?.url

    companion object {
        @Suppress("LongParameterList")
        fun seedFromCrawl(
            naverPlaceId: String,
            name: String,
            phoneNumber: String?,
            address: String,
            addressDetail: String?,
            latitude: Double?,
            longitude: Double?,
            thumbnailS3Key: String?,
            visitorReviewCount: Int,
            blogReviewCount: Int,
            categories: List<KindergartenCategory>,
            businessHours: List<KindergartenBusinessHour>,
            links: List<KindergartenLink>,
            options: List<KindergartenOption>,
            priceImages: List<KindergartenPriceImage>,
            menus: List<KindergartenMenu>,
        ): Kindergarten =
            Kindergarten(
                id = null,
                naverPlaceId = naverPlaceId,
                name = name,
                phoneNumber = phoneNumber,
                address = address,
                addressDetail = addressDetail,
                latitude = latitude,
                longitude = longitude,
                thumbnailS3Key = thumbnailS3Key,
                visitorReviewCount = visitorReviewCount,
                blogReviewCount = blogReviewCount,
                source = KindergartenSource.CRAWLED,
                status = KindergartenStatus.ACTIVE,
                categories = categories,
                businessHours = businessHours,
                links = links,
                options = options,
                priceImages = priceImages,
                menus = menus,
            )

        @Suppress("LongParameterList")
        fun reconstitute(
            id: KindergartenId,
            naverPlaceId: String?,
            name: String,
            phoneNumber: String?,
            address: String,
            addressDetail: String?,
            latitude: Double?,
            longitude: Double?,
            thumbnailS3Key: String?,
            visitorReviewCount: Int,
            blogReviewCount: Int,
            source: KindergartenSource,
            status: KindergartenStatus,
            categories: List<KindergartenCategory>,
            businessHours: List<KindergartenBusinessHour>,
            links: List<KindergartenLink>,
            options: List<KindergartenOption>,
            priceImages: List<KindergartenPriceImage>,
            menus: List<KindergartenMenu>,
        ): Kindergarten =
            Kindergarten(
                id,
                naverPlaceId,
                name,
                phoneNumber,
                address,
                addressDetail,
                latitude,
                longitude,
                thumbnailS3Key,
                visitorReviewCount,
                blogReviewCount,
                source,
                status,
                categories,
                businessHours,
                links,
                options,
                priceImages,
                menus,
            )
    }
}
