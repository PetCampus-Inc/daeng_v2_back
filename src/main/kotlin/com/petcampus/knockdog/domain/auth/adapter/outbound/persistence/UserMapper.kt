package com.petcampus.knockdog.domain.auth.adapter.outbound.persistence

import com.petcampus.knockdog.domain.auth.domain.User
import com.petcampus.knockdog.domain.auth.domain.UserAddress
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.auth.domain.UserId

/** 정석형: 순수 도메인 ↔ JPA 엔티티 변환. JPA는 이 어댑터 안에만 존재한다. */
fun User.toJpaEntity(): UserJpaEntity {
    val entity =
        UserJpaEntity(
            id = id?.value,
            userCode = code.value,
            nickname = nickname,
            profileImage = profileImage,
            infoReceiveEmail = infoReceiveEmail,
            gender = gender,
            phoneNumber = phoneNumber,
            emergencyPhoneNumber = emergencyPhoneNumber,
            deletedAt = deletedAt,
        )
    addresses.forEach { entity.addresses.add(it.toJpaEntity(entity)) }
    return entity
}

fun UserJpaEntity.toDomain(): User =
    User.reconstitute(
        id = UserId(requireNotNull(id) { "저장되지 않은 UserJpaEntity입니다." }),
        code = UserCode(userCode),
        nickname = nickname,
        profileImage = profileImage,
        infoReceiveEmail = infoReceiveEmail,
        gender = gender,
        phoneNumber = phoneNumber,
        emergencyPhoneNumber = emergencyPhoneNumber,
        addresses = addresses.map { it.toDomain() },
        deletedAt = deletedAt,
    )

private fun UserAddress.toJpaEntity(user: UserJpaEntity): UserAddressJpaEntity =
    UserAddressJpaEntity(
        id = id,
        user = user,
        type = type,
        alias = alias,
        address = address,
        roadAddress = roadAddress,
        addressDetail = addressDetail,
        lat = lat,
        lng = lng,
    )

private fun UserAddressJpaEntity.toDomain(): UserAddress =
    UserAddress.reconstitute(
        id = requireNotNull(id) { "저장되지 않은 UserAddressJpaEntity입니다." },
        type = type,
        alias = alias,
        address = address,
        roadAddress = roadAddress,
        addressDetail = addressDetail,
        lat = lat,
        lng = lng,
    )
