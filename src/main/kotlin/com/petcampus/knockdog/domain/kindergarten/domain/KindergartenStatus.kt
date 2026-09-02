package com.petcampus.knockdog.domain.kindergarten.domain

/**
 * 폐업 여부. 크롤링 시점엔 존재가 확인된 유치원만 담기므로 시딩 시 항상 ACTIVE로 들어간다.
 * CLOSED로 갱신하는 재크롤링 로직은 이번 범위 밖(docs/work/KD3-413-kindergarten-static-lookup.md).
 */
enum class KindergartenStatus {
    ACTIVE,
    CLOSED,
}
