package com.petcampus.knockdog.domain.auth.application.port.output

import com.petcampus.knockdog.domain.auth.domain.User

interface SaveUserPort {
    fun save(user: User): User
}
