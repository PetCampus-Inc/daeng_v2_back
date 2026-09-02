package com.petcampus.knockdog.domain.kindergarten.domain

/**
 * 옵션(태그) 하나. `code`는 크롤링 값을 그대로 담는 열린 문자열이라, 그룹 내 노출 순서는 DB 컬럼이 아니라
 * [KindergartenOptionGroup]별 고정 순서(코드 레벨)로 정렬한다 — 유치원마다 달라지는 값이 아니기 때문이다.
 */
data class KindergartenOption(
    val group: KindergartenOptionGroup,
    val code: String,
)
