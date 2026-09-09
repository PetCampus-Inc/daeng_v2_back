package com.petcampus.knockdog.domain.auth.application.port.output

import com.petcampus.knockdog.domain.auth.domain.UserId

interface LockUserPort {
    fun lockById(userId: UserId)
}
