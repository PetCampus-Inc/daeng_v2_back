package com.petcampus.knockdog.domain.auth.application.service

import com.petcampus.knockdog.domain.auth.application.AuthErrorCode
import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserPort
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.auth.domain.UserId
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.stereotype.Component

@Component
class RequireUserId(
    private val loadUserPort: LoadUserPort,
) {
    operator fun invoke(userCode: UserCode): UserId {
        val user = loadUserPort.findByCode(userCode) ?: throw BusinessException(AuthErrorCode.NOT_FOUND_USER)
        return requireNotNull(user.id) { "저장되지 않은 User입니다." }
    }
}
