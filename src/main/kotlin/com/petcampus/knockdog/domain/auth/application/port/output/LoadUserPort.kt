package com.petcampus.knockdog.domain.auth.application.port.output

import com.petcampus.knockdog.domain.auth.domain.User
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.auth.domain.UserId

interface LoadUserPort {
    fun findById(id: UserId): User?

    fun findByCode(code: UserCode): User?
}
