package com.petcampus.knockdog.domain.pet.application.service

import com.petcampus.knockdog.domain.auth.application.service.RequireUserId
import com.petcampus.knockdog.domain.pet.application.PetErrorCode
import com.petcampus.knockdog.domain.pet.application.port.input.GetPetCommand
import com.petcampus.knockdog.domain.pet.application.port.input.GetPetResult
import com.petcampus.knockdog.domain.pet.application.port.input.GetPetUseCase
import com.petcampus.knockdog.domain.pet.application.port.output.LoadBreedPort
import com.petcampus.knockdog.domain.pet.application.port.output.LoadPetPort
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetPetService(
    private val requireUserId: RequireUserId,
    private val loadPetPort: LoadPetPort,
    private val loadBreedPort: LoadBreedPort,
) : GetPetUseCase {
    @Transactional(readOnly = true)
    override fun getPet(command: GetPetCommand): GetPetResult {
        val userId = requireUserId(command.userCode)
        val pet =
            loadPetPort.findById(command.petId)?.takeIf { !it.isDeleted }
                ?: throw BusinessException(PetErrorCode.NOT_FOUND)
        if (pet.userId != userId) throw BusinessException(PetErrorCode.NOT_AUTHORIZED)

        val breed =
            checkNotNull(loadBreedPort.findById(pet.breedId)) {
                "pet(${pet.id?.value})이 참조하는 breed(${pet.breedId})가 존재하지 않습니다."
            }

        return GetPetResult(pet, breed)
    }
}
