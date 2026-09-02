package com.petcampus.knockdog.domain.kindergarten.domain

/**
 * 유치원 정적 정보 애그리거트 루트. 정적 조회(요약/상세/요금표)에 필요한 만큼만 담는다 —
 * 지도/좌표 기반 조회(map-view 등)에 필요한 필드는 후속 하위 작업에서 추가한다.
 */
class Kindergarten private constructor(
    val id: KindergartenId?,
    val naverPlaceId: String?,
    val name: String,
    val phoneNumber: String?,
    val address: String,
    val roadAddress: String?,
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
    /** 레거시 `reviewCount` 계산식(blog+visitor, 음수 방어)을 그대로 이식했다. */
    val reviewCount: Int
        get() = visitorReviewCount.coerceAtLeast(0) + blogReviewCount.coerceAtLeast(0)

    /** 레거시 `PriceUtils.getLowestPrice` — 메뉴 중 최저가, 없으면 0. */
    val lowestPrice: Int
        get() = menus.mapNotNull { it.price }.minOrNull() ?: 0

    fun optionsOf(group: KindergartenOptionGroup): List<KindergartenOption> = options.filter { it.group == group }.sortedBy { it.code }

    fun linkOf(code: String): String? = links.firstOrNull { it.code.equals(code, ignoreCase = true) }?.url

    companion object {
        /** 크롤링 JSON 시딩 전용 — 신규 유치원 생성. */
        @Suppress("LongParameterList")
        fun seedFromCrawl(
            naverPlaceId: String,
            name: String,
            phoneNumber: String?,
            address: String,
            roadAddress: String?,
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
                roadAddress = roadAddress,
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

        /** 영속성에서 복원할 때 사용(매퍼 전용). */
        @Suppress("LongParameterList")
        fun reconstitute(
            id: KindergartenId,
            naverPlaceId: String?,
            name: String,
            phoneNumber: String?,
            address: String,
            roadAddress: String?,
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
                roadAddress,
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
