package com.petcampus.knockdog.domain.owner.application.port.output

import com.petcampus.knockdog.domain.owner.domain.Owner

interface SaveOwnerPort {
    fun save(owner: Owner): Owner
}
