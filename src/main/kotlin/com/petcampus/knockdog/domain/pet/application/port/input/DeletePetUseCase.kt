package com.petcampus.knockdog.domain.pet.application.port.input

import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.pet.domain.PetId

interface DeletePetUseCase {
    fun delete(command: DeletePetCommand)
}

data class DeletePetCommand(
    val userCode: UserCode,
    val petId: PetId,
)
