package com.petcampus.knockdog.domain.owner.application.service

import com.petcampus.knockdog.domain.owner.application.port.input.GetOwnerUseCase
import com.petcampus.knockdog.domain.owner.application.port.output.LoadOwnerPort
import com.petcampus.knockdog.domain.owner.domain.Owner
import com.petcampus.knockdog.domain.owner.domain.OwnerId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetOwnerService(
    private val loadOwnerPort: LoadOwnerPort,
) : GetOwnerUseCase {
    override fun getById(id: OwnerId): Owner =
        loadOwnerPort.findById(id)
            ?: throw NoSuchElementException("회원을 찾을 수 없습니다: ${id.value}")
}
