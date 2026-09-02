package com.petcampus.knockdog.domain.kindergarten.domain

/**
 * 유치원 옵션(태그)의 4개 분류. 크롤링 JSON의 서로 다른 배열(dog_breeds_accepted, dog_services,
 * dog_safety_facilities, visitor_amenities)에서 각각 채워지며, 그룹 자체는 우리가 정한 고정 분류라 enum으로 닫는다.
 * (그룹 *안의* option_code 값은 크롤링이 계속 새 값을 추가할 수 있어 String으로 열어둔다.)
 */
enum class KindergartenOptionGroup {
    DOG_BREED,
    DOG_SERVICE,
    SAFETY_FACILITY,
    VISITOR_AMENITY,
}
