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

/** `id`·`addressDetail`은 `v0` 계약이다 — 프론트가 `id`로 주소 수정·삭제를, `addressDetail`을 화면 표시에 쓴다. */
data class UserAddressResponse(
    val id: Long?,
    val type: AddressType,
    val alias: String?,
    val address: String,
    val roadAddress: String?,
    val addressDetail: String?,
    val lat: Double,
    val lng: Double,
) {
    companion object {
        fun from(address: UserAddress): UserAddressResponse =
            UserAddressResponse(
                id = address.id,
                type = address.type,
                alias = address.alias,
                address = address.address,
                roadAddress = address.roadAddress,
                addressDetail = address.addressDetail,
                lat = address.lat,
                lng = address.lng,
            )
    }
}
