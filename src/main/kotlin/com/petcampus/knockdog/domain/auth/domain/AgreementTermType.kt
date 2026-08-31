package com.petcampus.knockdog.domain.auth.domain

/**
 * 레거시 `AgreementTermType`과 동일한 enum 이름을 유지한다 — 프론트가 이 값을 그대로 요청 본문에 싣는다
 * (`POST /api/v0/user/agreements`는 KEEP, docs/rules/api-migration.md §2 계약 보존).
 */
enum class AgreementTermType {
    TERMS_OF_SERVICE,
    PRIVACY_POLICY,
    AGE_OVER_14,
    ;

    companion object {
        /** 가입에 반드시 필요한 약관. 하나라도 빠지면 동의 제출을 거부한다. */
        val REQUIRED: Set<AgreementTermType> = setOf(TERMS_OF_SERVICE, PRIVACY_POLICY, AGE_OVER_14)
    }
}
