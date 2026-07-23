package com.petcampus.knockdog.domain.owner.domain

/**
 * 정석형 순수 도메인 모델. JPA/Spring을 전혀 모른다.
 * 항상 유효한 상태(always-valid)를 팩토리 메서드로 강제한다.
 */
class Owner private constructor(
    val id: OwnerId,
    val email: Email,
    status: OwnerStatus,
) {
    var status: OwnerStatus = status
        private set

    fun withdraw() {
        require(status == OwnerStatus.ACTIVE) { "이미 탈퇴한 회원입니다." }
        status = OwnerStatus.WITHDRAWN
    }

    companion object {
        /** 신규 등록: 항상 ACTIVE로 시작한다. */
        fun register(email: Email): Owner = Owner(OwnerId.generate(), email, OwnerStatus.ACTIVE)

        /** 영속성에서 복원할 때 사용(매퍼 전용). */
        fun reconstitute(
            id: OwnerId,
            email: Email,
            status: OwnerStatus,
        ): Owner = Owner(id, email, status)
    }
}
