package com.petcampus.knockdog.domain.auth.domain

import java.time.LocalDateTime

/**
 * 정석형 순수 도메인 모델. JPA/Spring을 전혀 모른다.
 * 탈퇴 여부는 별도 status 없이 deletedAt으로 표현한다 (docs/conventions/jpa-entity.md).
 */
class User private constructor(
    val id: UserId?,
    val code: UserCode,
    val nickname: String?,
    val profileImage: String?,
    val infoReceiveEmail: String?,
    val gender: String?,
    val phoneNumber: String?,
    val emergencyPhoneNumber: String?,
    val addresses: List<UserAddress>,
    deletedAt: LocalDateTime?,
) {
    var deletedAt: LocalDateTime? = deletedAt
        private set

    val isWithdrawn: Boolean
        get() = deletedAt != null

    fun withdraw() {
        check(!isWithdrawn) { "이미 탈퇴한 회원입니다." }
        deletedAt = LocalDateTime.now()
    }

    companion object {
        /** 신규 회원가입: 주소 목록 중 HOME 타입이 최소 1개 있어야 한다(불변식). */
        fun create(
            nickname: String?,
            profileImage: String?,
            infoReceiveEmail: String?,
            addresses: List<UserAddress>,
        ): User {
            require(addresses.any { it.type == AddressType.HOME }) { "HOME 타입 주소가 최소 1개 있어야 합니다." }

            return User(
                id = null,
                code = UserCode.generate(),
                nickname = nickname,
                profileImage = profileImage,
                infoReceiveEmail = infoReceiveEmail,
                gender = null,
                phoneNumber = null,
                emergencyPhoneNumber = null,
                addresses = addresses,
                deletedAt = null,
            )
        }

        /** 영속성에서 복원할 때 사용(매퍼 전용). */
        fun reconstitute(
            id: UserId,
            code: UserCode,
            nickname: String?,
            profileImage: String?,
            infoReceiveEmail: String?,
            gender: String?,
            phoneNumber: String?,
            emergencyPhoneNumber: String?,
            addresses: List<UserAddress>,
            deletedAt: LocalDateTime?,
        ): User =
            User(
                id,
                code,
                nickname,
                profileImage,
                infoReceiveEmail,
                gender,
                phoneNumber,
                emergencyPhoneNumber,
                addresses,
                deletedAt,
            )
    }
}
