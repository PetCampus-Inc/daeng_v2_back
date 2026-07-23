package com.petcampus.knockdog.domain.owner.adapter.outbound.persistence

import com.petcampus.knockdog.domain.owner.domain.Email
import com.petcampus.knockdog.domain.owner.domain.Owner
import com.petcampus.knockdog.domain.owner.domain.OwnerId

/** 정석형: 순수 도메인 ↔ JPA 엔티티 변환. JPA는 이 어댑터 안에만 존재한다. */
fun Owner.toJpaEntity(): OwnerJpaEntity =
    OwnerJpaEntity(
        id = id.value,
        email = email.value,
        status = status,
    )

fun OwnerJpaEntity.toDomain(): Owner =
    Owner.reconstitute(
        id = OwnerId(id),
        email = Email(email),
        status = status,
    )
