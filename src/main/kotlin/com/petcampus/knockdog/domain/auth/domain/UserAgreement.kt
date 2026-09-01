package com.petcampus.knockdog.domain.auth.domain

import java.time.LocalDateTime

/** 약관 1건에 대한 동의 이력. (userId, termType)이 유일하며 동의 시각은 최초 1회만 남는다. */
class UserAgreement private constructor(
    val userId: UserId,
    val termType: AgreementTermType,
    val agreedAt: LocalDateTime,
) {
    companion object {
        fun create(
            userId: UserId,
            termType: AgreementTermType,
            agreedAt: LocalDateTime,
        ): UserAgreement = UserAgreement(userId, termType, agreedAt)
    }
}
