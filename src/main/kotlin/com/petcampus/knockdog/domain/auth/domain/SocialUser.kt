package com.petcampus.knockdog.domain.auth.domain

import java.time.LocalDateTime

class SocialUser private constructor(
    val id: SocialUserId?,
    val provider: Provider,
    val providerId: String,
    val email: String,
    val name: String?,
    val picture: String?,
    status: SocialUserStatus,
    userId: UserId?,
    linkedAt: LocalDateTime?,
) {
    var status: SocialUserStatus = status
        private set

    var userId: UserId? = userId
        private set

    var linkedAt: LocalDateTime? = linkedAt
        private set

    val isLinked: Boolean
        get() = status == SocialUserStatus.LINKED

    /** 회원(User)과 연동한다 — 회원가입 또는 재연동 시 사용. */
    fun link(userId: UserId) {
        this.userId = userId
        this.status = SocialUserStatus.LINKED
        this.linkedAt = LocalDateTime.now()
    }

    /** 연동을 해제한다 — 탈퇴 후 재가입 제한 기간이 지난 계정에 사용. */
    fun unlink() {
        this.userId = null
        this.status = SocialUserStatus.UNLINKED
    }

    companion object {
        /** 신규 소셜 계정 생성: 최초 OIDC 검증 시(A-1) UNLINKED 또는 PENDING 상태로 만들어진다. */
        fun create(
            provider: Provider,
            providerId: String,
            email: String,
            name: String?,
            picture: String?,
            status: SocialUserStatus,
        ): SocialUser = SocialUser(null, provider, providerId, email, name, picture, status, null, null)

        /** 영속성에서 복원할 때 사용(매퍼 전용). */
        fun reconstitute(
            id: SocialUserId,
            provider: Provider,
            providerId: String,
            email: String,
            name: String?,
            picture: String?,
            status: SocialUserStatus,
            userId: UserId?,
            linkedAt: LocalDateTime?,
        ): SocialUser = SocialUser(id, provider, providerId, email, name, picture, status, userId, linkedAt)
    }
}
