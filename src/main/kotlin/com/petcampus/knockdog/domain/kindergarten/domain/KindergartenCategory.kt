package com.petcampus.knockdog.domain.kindergarten.domain

/** 업종 카테고리 하나(예: KINDERGARTEN, HOTEL). 크롤링 값이 계속 늘 수 있어 닫힌 enum이 아니라 String으로 연다. */
data class KindergartenCategory(
    val value: String,
)
