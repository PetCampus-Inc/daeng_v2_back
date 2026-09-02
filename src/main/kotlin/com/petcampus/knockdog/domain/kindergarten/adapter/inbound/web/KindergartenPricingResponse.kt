package com.petcampus.knockdog.domain.kindergarten.adapter.inbound.web

import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten

/**
 * `GET /api/v0/kindergarten/{id}/pricing` 응답 — 레거시 `KindergartenPricingInfoResponse`와 계약을 맞춘다.
 *
 * 알려진 근사치: `count`는 레거시가 `Menu`(내부 파싱 객체)의 `totalTime`/`count` 필드를 조합해 만드는데
 * 정확한 필드 대응을 확인하지 못해, 크롤링 원본의 `unit_str`(예: "5시간")을 그대로 쓴다. 로컬 응답 대조
 * 시 실제 값이 다르면 이 부분을 재확인한다(docs/work/KD3-413-kindergarten-static-lookup.md).
 */
data class KindergartenPricingResponse(
    val id: String,
    val productType: List<String>,
    val productCategories: List<ProductCategory>,
    val phoneNumber: String?,
    val priceImages: List<String>,
    val lastUpdatedAt: String,
) {
    data class ProductCategory(
        val productName: String,
        val products: List<ProductItem>,
    )

    data class ProductItem(
        val price: String?,
        val count: String?,
        val weightSection: String?,
    )

    companion object {
        private const val DEFAULT_LAST_UPDATED_AT_DISPLAY = "정보 없음"

        fun from(kindergarten: Kindergarten): KindergartenPricingResponse {
            val productType = kindergarten.menus.map { it.productType }.distinct()
            val productCategories =
                kindergarten.menus
                    .sortedBy { it.displayOrder }
                    .groupBy { it.productName }
                    .map { (productName, menus) ->
                        ProductCategory(
                            productName = productName,
                            products =
                                menus.map {
                                    ProductItem(
                                        price = it.price?.let { price -> "%,d원".format(price) },
                                        count = it.unitStr ?: it.totalDurationStr,
                                        weightSection = it.weightRange,
                                    )
                                },
                        )
                    }

            return KindergartenPricingResponse(
                id = requireNotNull(kindergarten.naverPlaceId),
                productType = productType,
                productCategories = productCategories,
                phoneNumber = kindergarten.phoneNumber,
                priceImages = kindergarten.priceImages.sortedBy { it.displayOrder }.map { it.s3Key },
                lastUpdatedAt = DEFAULT_LAST_UPDATED_AT_DISPLAY,
            )
        }
    }
}
