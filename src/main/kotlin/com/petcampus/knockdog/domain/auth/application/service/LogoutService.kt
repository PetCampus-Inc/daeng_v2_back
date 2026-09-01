package com.petcampus.knockdog.domain.auth.application.service

import com.petcampus.knockdog.domain.auth.application.port.input.LogoutCommand
import com.petcampus.knockdog.domain.auth.application.port.input.LogoutUseCase
import com.petcampus.knockdog.domain.auth.application.port.output.DeleteRefreshTokenPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LogoutService(
    private val deleteRefreshTokenPort: DeleteRefreshTokenPort,
) : LogoutUseCase {
    @Transactional
    override fun logout(command: LogoutCommand) {
        if (command.refreshToken.isBlank()) return
        deleteRefreshTokenPort.deleteByToken(command.refreshToken)
    }
}
