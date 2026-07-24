package com.petcampus.knockdog.domain.owner.application.service

import com.petcampus.knockdog.domain.owner.application.port.input.RegisterOwnerCommand
import com.petcampus.knockdog.domain.owner.application.port.input.RegisterOwnerUseCase
import com.petcampus.knockdog.domain.owner.application.port.output.SaveOwnerPort
import com.petcampus.knockdog.domain.owner.domain.Email
import com.petcampus.knockdog.domain.owner.domain.Owner
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterOwnerService(
    private val saveOwnerPort: SaveOwnerPort,
) : RegisterOwnerUseCase {
    @Transactional
    override fun register(command: RegisterOwnerCommand): Owner {
        val owner = Owner.register(Email(command.email))
        return saveOwnerPort.save(owner)
    }
}
