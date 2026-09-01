package com.petcampus.knockdog.domain.auth.adapter.outbound.persistence

import com.petcampus.knockdog.domain.auth.domain.SocialUser
import com.petcampus.knockdog.domain.auth.domain.SocialUserId
import com.petcampus.knockdog.domain.auth.domain.UserId

/** userRef: 크로스 애그리게잇 참조라 엔티티 전체 로딩 없이 어댑터가 만든 프록시 참조(entityManager.getReference)를 그대로 받는다. */
fun SocialUser.toJpaEntity(userRef: UserJpaEntity?): SocialUserJpaEntity =
    SocialUserJpaEntity(
        id = id?.value,
        provider = provider,
        providerId = providerId,
        email = email,
        name = name,
        picture = picture,
        status = status,
        user = userRef,
        linkedAt = linkedAt,
    )

fun SocialUserJpaEntity.toDomain(): SocialUser =
    SocialUser.reconstitute(
        id = SocialUserId(requireNotNull(id) { "저장되지 않은 SocialUserJpaEntity입니다." }),
        provider = provider,
        providerId = providerId,
        email = email,
        name = name,
        picture = picture,
        status = status,
        userId = user?.id?.let { UserId(it) },
        linkedAt = linkedAt,
    )
