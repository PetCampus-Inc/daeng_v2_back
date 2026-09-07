package com.petcampus.knockdog.domain.auth.application.port.output

interface LockUserPort {
    fun lockById(userId: Long)
}
