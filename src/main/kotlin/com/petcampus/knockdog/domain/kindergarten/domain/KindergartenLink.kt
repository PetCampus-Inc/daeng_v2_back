package com.petcampus.knockdog.domain.kindergarten.domain

/** 홈페이지/SNS 링크 하나. `code`는 크롤링 값을 그대로 담는 열린 문자열이다(HOMEPAGE, INSTAGRAM, YOUTUBE, BLOG 등). */
data class KindergartenLink(
    val code: String,
    val url: String,
)
