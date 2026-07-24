package com.petcampus.knockdog.domain.owner.adapter.outbound.persistence

import com.petcampus.knockdog.domain.owner.application.port.output.LoadOwnerPort
import com.petcampus.knockdog.domain.owner.application.port.output.SaveOwnerPort
import com.petcampus.knockdog.domain.owner.domain.Owner
import com.petcampus.knockdog.domain.owner.domain.OwnerId
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

/** 출력 포트(Save/Load)의 구현체. 도메인과 JPA 사이를 매퍼로 잇는다. */
@Component
class OwnerPersistenceAdapter(
    private val ownerJpaRepository: OwnerJpaRepository,
) : SaveOwnerPort,
    LoadOwnerPort {
    override fun save(owner: Owner): Owner = ownerJpaRepository.save(owner.toJpaEntity()).toDomain()

    override fun findById(id: OwnerId): Owner? = ownerJpaRepository.findByIdOrNull(id.value)?.toDomain()
}
