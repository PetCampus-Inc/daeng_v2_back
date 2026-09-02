package com.petcampus.knockdog.domain.kindergarten.domain

/** 데이터 출처. 크롤링 vs 원장 자체 등록(네이버 미등재) 구분 (docs/adr/0011). */
enum class KindergartenSource {
    CRAWLED,
    OWNER_REGISTERED,
}
