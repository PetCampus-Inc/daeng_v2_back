package com.petcampus.knockdog.domain.pet.application.service

import com.petcampus.knockdog.domain.auth.application.AuthErrorCode
import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserPort
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.pet.application.PetErrorCode
import com.petcampus.knockdog.domain.pet.application.port.input.SetRepresentativeCommand
import com.petcampus.knockdog.domain.pet.application.port.input.SetRepresentativeResult
import com.petcampus.knockdog.domain.pet.application.port.input.SetRepresentativeUseCase
import com.petcampus.knockdog.domain.pet.application.port.output.LoadBreedPort
import com.petcampus.knockdog.domain.pet.application.port.output.LoadPetPort
import com.petcampus.knockdog.domain.pet.application.port.output.SavePetPort
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SetRepresentativeService(
    private val loadUserPort: LoadUserPort,
    private val loadPetPort: LoadPetPort,
    private val loadBreedPort: LoadBreedPort,
    private val savePetPort: SavePetPort,
) : SetRepresentativeUseCase {
    @Transactional
    override fun setRepresentative(command: SetRepresentativeCommand): SetRepresentativeResult {
        val userId = requireUserId(command.userCode)
        val pet =
            loadPetPort.findById(command.petId)?.takeIf { !it.isDeleted }
                ?: throw BusinessException(PetErrorCode.NOT_FOUND)
        if (pet.userId != userId) throw BusinessException(PetErrorCode.NOT_AUTHORIZED)

        val updated = savePetPort.setRepresentativeWithinLock(pet)
        val breed =
            checkNotNull(loadBreedPort.findById(updated.breedId)) {
                "pet(${updated.id?.value})이 참조하는 breed(${updated.breedId})가 존재하지 않습니다."
            }

        return SetRepresentativeResult(updated, breed)
    }

    private fun requireUserId(userCode: UserCode): Long {
        val user = loadUserPort.findByCode(userCode) ?: throw BusinessException(AuthErrorCode.NOT_FOUND_USER)
        return requireNotNull(user.id) { "저장되지 않은 User입니다." }.value
    }
}
