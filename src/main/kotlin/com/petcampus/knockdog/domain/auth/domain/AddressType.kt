package com.petcampus.knockdog.domain.auth.domain

/**
 * 레거시가 KD3-372에서 WORK를 OTHER로 통합하고 기존 데이터도 마이그레이션했다.
 * 제품이 없앤 타입이므로 신규 서버에서도 되살리지 않는다.
 */
enum class AddressType {
    HOME,
    OTHER,
}
