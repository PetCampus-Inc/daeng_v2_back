package com.petcampus.knockdog.domain.pet.application.service

import com.petcampus.knockdog.domain.auth.application.port.output.LockUserPort
import com.petcampus.knockdog.domain.pet.application.port.output.LoadPetPort
import com.petcampus.knockdog.domain.pet.domain.Pet
import org.springframework.stereotype.Component

@Component
class PetLockOperations(
    private val lockUserPort: LockUserPort,
    private val loadPetPort: LoadPetPort,
) {
    fun <T> withLockedActivePets(
        userId: Long,
        block: (List<Pet>) -> T,
    ): T {
        lockUserPort.lockById(userId)
        return block(loadPetPort.findAllActiveByUserIdForUpdate(userId))
    }
}
