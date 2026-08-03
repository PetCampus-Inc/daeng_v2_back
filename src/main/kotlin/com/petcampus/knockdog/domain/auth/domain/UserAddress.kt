package com.petcampus.knockdog.domain.auth.domain

class UserAddress private constructor(
    val type: AddressType,
    val alias: String?,
    val address: String,
    val roadAddress: String?,
    val lat: Double,
    val lng: Double,
) {
    companion object {
        fun create(
            type: AddressType,
            alias: String?,
            address: String,
            roadAddress: String?,
            lat: Double,
            lng: Double,
        ): UserAddress = UserAddress(type, alias, address, roadAddress, lat, lng)
    }
}
