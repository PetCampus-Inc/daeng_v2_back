package com.petcampus.knockdog.domain.auth.application.port.output

import com.petcampus.knockdog.domain.auth.domain.UserCode

interface DeleteRefreshTokenPort {
    fun deleteByToken(token: String)

    fun deleteAllByUserCode(userCode: UserCode)
}
