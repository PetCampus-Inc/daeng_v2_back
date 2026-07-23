package com.petcampus.knockdog.domain.owner.application.port.input

import com.petcampus.knockdog.domain.owner.domain.Owner

interface RegisterOwnerUseCase {
    fun register(command: RegisterOwnerCommand): Owner
}

data class RegisterOwnerCommand(
    val email: String,
)
