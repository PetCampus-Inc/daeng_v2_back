package com.petcampus.knockdog.domain.kindergarten.adapter.outbound.persistence

import com.petcampus.knockdog.domain.kindergarten.domain.Kindergarten
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenBusinessHour
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenCategory
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenId
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenLink
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenMenu
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenOption
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenOptionGroup
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenPriceImage
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenSource
import com.petcampus.knockdog.domain.kindergarten.domain.KindergartenStatus

fun KindergartenJpaEntity.toDomain(
    categories: List<KindergartenCategoryJpaEntity>,
    businessHours: List<KindergartenBusinessHourJpaEntity>,
    links: List<KindergartenLinkJpaEntity>,
    options: List<KindergartenOptionJpaEntity>,
    priceImages: List<KindergartenPriceImageJpaEntity>,
    menus: List<KindergartenMenuJpaEntity>,
): Kindergarten =
    Kindergarten.reconstitute(
        id = KindergartenId(requireNotNull(id) { "저장되지 않은 KindergartenJpaEntity입니다." }),
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
        source = KindergartenSource.valueOf(source),
        status = KindergartenStatus.valueOf(status),
        categories = categories.map { KindergartenCategory(it.category) },
        businessHours = businessHours.map { it.toDomain() },
        links = links.map { KindergartenLink(it.code, it.url) },
        options = options.map { KindergartenOption(KindergartenOptionGroup.valueOf(it.optionGroup), it.optionCode) },
        priceImages = priceImages.map { KindergartenPriceImage(it.s3Key, it.displayOrder) },
        menus = menus.map { it.toDomain() },
    )

fun Kindergarten.toJpaEntity(): KindergartenJpaEntity =
    KindergartenJpaEntity(
        id = id?.value,
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
        source = source.name,
        status = status.name,
    )

fun Kindergarten.toCategoryJpaEntities(kindergartenId: Long): List<KindergartenCategoryJpaEntity> =
    categories.map { KindergartenCategoryJpaEntity(kindergartenId = kindergartenId, category = it.value) }

fun Kindergarten.toBusinessHourJpaEntities(kindergartenId: Long): List<KindergartenBusinessHourJpaEntity> =
    businessHours.map { it.toJpaEntity(kindergartenId) }

fun Kindergarten.toLinkJpaEntities(kindergartenId: Long): List<KindergartenLinkJpaEntity> =
    links.map { KindergartenLinkJpaEntity(kindergartenId = kindergartenId, code = it.code, url = it.url) }

fun Kindergarten.toOptionJpaEntities(kindergartenId: Long): List<KindergartenOptionJpaEntity> =
    options.map { KindergartenOptionJpaEntity(kindergartenId = kindergartenId, optionGroup = it.group.name, optionCode = it.code) }

fun Kindergarten.toPriceImageJpaEntities(kindergartenId: Long): List<KindergartenPriceImageJpaEntity> =
    priceImages.map { KindergartenPriceImageJpaEntity(kindergartenId = kindergartenId, s3Key = it.s3Key, displayOrder = it.displayOrder) }

fun Kindergarten.toMenuJpaEntities(kindergartenId: Long): List<KindergartenMenuJpaEntity> = menus.map { it.toJpaEntity(kindergartenId) }

private fun KindergartenBusinessHourJpaEntity.toDomain(): KindergartenBusinessHour =
    KindergartenBusinessHour(
        name = name,
        weekdayOpen = weekdayOpen,
        weekdayClose = weekdayClose,
        weekendOpen = weekendOpen,
        weekendClose = weekendClose,
        offdays = offdays,
    )

private fun KindergartenBusinessHour.toJpaEntity(kindergartenId: Long): KindergartenBusinessHourJpaEntity =
    KindergartenBusinessHourJpaEntity(
        kindergartenId = kindergartenId,
        name = name,
        weekdayOpen = weekdayOpen,
        weekdayClose = weekdayClose,
        weekendOpen = weekendOpen,
        weekendClose = weekendClose,
        offdays = offdays,
    )

private fun KindergartenMenuJpaEntity.toDomain(): KindergartenMenu =
    KindergartenMenu(
        productType = productType,
        serviceType = serviceType,
        productName = productName,
        unit = unit,
        unitStr = unitStr,
        unitType = unitType,
        weightRange = weightRange,
        price = price,
        hourlyPrice = hourlyPrice,
        isMinPrice = isMinPrice,
        isMaxPrice = isMaxPrice,
        totalDurationStr = totalDurationStr,
        totalDurationMinutes = totalDurationMinutes,
        displayOrder = displayOrder,
    )

private fun KindergartenMenu.toJpaEntity(kindergartenId: Long): KindergartenMenuJpaEntity =
    KindergartenMenuJpaEntity(
        kindergartenId = kindergartenId,
        productType = productType,
        serviceType = serviceType,
        productName = productName,
        unit = unit,
        unitStr = unitStr,
        unitType = unitType,
        weightRange = weightRange,
        price = price,
        hourlyPrice = hourlyPrice,
        isMinPrice = isMinPrice,
        isMaxPrice = isMaxPrice,
        totalDurationStr = totalDurationStr,
        totalDurationMinutes = totalDurationMinutes,
        displayOrder = displayOrder,
    )
