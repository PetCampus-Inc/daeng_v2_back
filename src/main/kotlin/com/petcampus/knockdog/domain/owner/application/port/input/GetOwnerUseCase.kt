package com.petcampus.knockdog.domain.owner.application.port.input

import com.petcampus.knockdog.domain.owner.domain.Owner
import com.petcampus.knockdog.domain.owner.domain.OwnerId

interface GetOwnerUseCase {
    fun getById(id: OwnerId): Owner
}
