package com.petcampus.knockdog.domain.kindergarten.domain

/** 요금표 이미지 하나. 크롤링 원본은 `info_new.json`의 `menu_image_s3_keys` 배열이다. */
data class KindergartenPriceImage(
    val s3Key: String,
    val displayOrder: Int,
)
