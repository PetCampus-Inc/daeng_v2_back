package com.petcampus.knockdog.domain.auth.domain

/**
 * `id`와 `addressDetail`은 `v0` 계약 때문에 존재한다 — 레거시가 `UserAddress` JPA 엔티티를
 * 그대로 직렬화해 내려주고, 프론트가 `id`로 주소 수정·삭제를 하고 `addressDetail`을 화면에 쓴다
 * (`MypageProfileLocationPage.tsx`, `LocationField.tsx`). 내부 PK를 노출하는 형태라
 * 좋은 설계는 아니지만 계약 보존 대상이라 뺄 수 없다.
 */
class UserAddress private constructor(
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
        fun create(
            type: AddressType,
            alias: String?,
            address: String,
            roadAddress: String?,
            lat: Double,
            lng: Double,
            addressDetail: String? = null,
        ): UserAddress = UserAddress(null, type, alias, address, roadAddress, addressDetail, lat, lng)

        /** 영속성에서 복원할 때 사용(매퍼 전용). */
        fun reconstitute(
            id: Long,
            type: AddressType,
            alias: String?,
            address: String,
            roadAddress: String?,
            addressDetail: String?,
            lat: Double,
            lng: Double,
        ): UserAddress = UserAddress(id, type, alias, address, roadAddress, addressDetail, lat, lng)
    }
}
