package com.petcampus.knockdog.domain.auth.domain

/** 내부 PK. FK나 토큰 subject 등 외부에 노출하지 않는다 — 외부 노출은 [UserCode]. */
@JvmInline
value class UserId(
    val value: Long,
)
