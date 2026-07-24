package com.petcampus.knockdog.domain.owner.application.port.output

import com.petcampus.knockdog.domain.owner.domain.Owner
import com.petcampus.knockdog.domain.owner.domain.OwnerId

interface LoadOwnerPort {
    fun findById(id: OwnerId): Owner?
}
