package com.petcampus.knockdog.domain.pet.application.service

import com.petcampus.knockdog.domain.auth.application.AuthErrorCode
import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserPort
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.pet.application.PetErrorCode
import com.petcampus.knockdog.domain.pet.application.port.input.DeletePetCommand
import com.petcampus.knockdog.domain.pet.application.port.input.DeletePetUseCase
import com.petcampus.knockdog.domain.pet.application.port.output.LoadPetPort
import com.petcampus.knockdog.domain.pet.application.port.output.SavePetPort
import com.petcampus.knockdog.domain.pet.domain.Pet
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeletePetService(
    private val loadUserPort: LoadUserPort,
    private val loadPetPort: LoadPetPort,
    private val savePetPort: SavePetPort,
    private val petLockOperations: PetLockOperations,
) : DeletePetUseCase {
    @Transactional
    override fun delete(command: DeletePetCommand) {
        val userId = requireUserId(command.userCode)
        val pet =
            loadPetPort.findById(command.petId)?.takeIf { !it.isDeleted }
                ?: throw BusinessException(PetErrorCode.NOT_FOUND)
        if (pet.userId != userId) throw BusinessException(PetErrorCode.NOT_AUTHORIZED)

        if (!pet.isRepresentative) {
            pet.delete()
            savePetPort.save(pet)
            return
        }

        petLockOperations.withLockedActivePets(userId) { activePets ->
            val target =
                activePets.find { it.id == pet.id }
                    ?: throw BusinessException(PetErrorCode.NOT_FOUND)
            val wasRepresentative = target.isRepresentative

            target.delete()
            savePetPort.saveAndFlush(target)

            if (wasRepresentative) {
                Pet
                    .selectNextRepresentative(activePets.filter { it.id != target.id })
                    ?.apply { markAsRepresentative() }
                    ?.let { savePetPort.save(it) }
            }
        }
    }

    private fun requireUserId(userCode: UserCode): Long {
        val user = loadUserPort.findByCode(userCode) ?: throw BusinessException(AuthErrorCode.NOT_FOUND_USER)
        return requireNotNull(user.id) { "저장되지 않은 User입니다." }.value
    }
}
