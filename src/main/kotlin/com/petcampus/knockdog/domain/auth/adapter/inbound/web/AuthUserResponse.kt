package com.petcampus.knockdog.domain.auth.adapter.inbound.web

import com.petcampus.knockdog.domain.auth.domain.AddressType
import com.petcampus.knockdog.domain.auth.domain.User
import com.petcampus.knockdog.domain.auth.domain.UserAddress

/** 레거시 AuthUserResponse와 동일한 필드 유지 — 로그인/회원가입 성공 시 프론트가 그대로 User 스토어에 저장한다. */
data class AuthUserResponse(
    val userId: String,
    val nickname: String?,
    val profileImage: String?,
    val addresses: List<UserAddressResponse>,
    val status: String,
) {
    companion object {
        private const val STATUS_ACTIVE = "ACTIVE"
        private const val STATUS_WITHDRAWN = "WITHDRAWN"

        fun from(user: User): AuthUserResponse =
            AuthUserResponse(
                userId = user.code.value,
                nickname = user.nickname,
                profileImage = user.profileImage,
                addresses = user.addresses.map { UserAddressResponse.from(it) },
                status = if (user.isWithdrawn) STATUS_WITHDRAWN else STATUS_ACTIVE,
            )
    }
}

data class UserAddressResponse(
    val type: AddressType,
    val alias: String?,
    val address: String,
    val roadAddress: String?,
    val lat: Double,
    val lng: Double,
) {
    companion object {
        fun from(address: UserAddress): UserAddressResponse =
            UserAddressResponse(
                type = address.type,
                alias = address.alias,
                address = address.address,
                roadAddress = address.roadAddress,
                lat = address.lat,
                lng = address.lng,
            )
    }
}
