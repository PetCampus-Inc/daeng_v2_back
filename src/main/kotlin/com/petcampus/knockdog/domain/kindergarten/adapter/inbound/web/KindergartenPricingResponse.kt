package com.petcampus.knockdog.domain.kindergarten.adapter.inbound.web

import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten

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
                                        count = it.unitLabel ?: it.totalDurationLabel,
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
