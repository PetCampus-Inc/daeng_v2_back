package com.petcampus.knockdog.domain.pet.application.service

import com.petcampus.knockdog.domain.auth.application.AuthErrorCode
import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserPort
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.pet.application.PetErrorCode
import com.petcampus.knockdog.domain.pet.application.port.input.CreatePetCommand
import com.petcampus.knockdog.domain.pet.application.port.input.CreatePetResult
import com.petcampus.knockdog.domain.pet.application.port.input.CreatePetUseCase
import com.petcampus.knockdog.domain.pet.application.port.output.LoadBreedPort
import com.petcampus.knockdog.domain.pet.application.port.output.SavePetPort
import com.petcampus.knockdog.domain.pet.domain.Pet
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreatePetService(
    private val loadUserPort: LoadUserPort,
    private val loadBreedPort: LoadBreedPort,
    private val savePetPort: SavePetPort,
) : CreatePetUseCase {
    @Transactional
    override fun create(command: CreatePetCommand): CreatePetResult {
        val userId = requireUserId(command.userCode)
        val breed = loadBreedPort.findById(command.breedId) ?: throw BusinessException(PetErrorCode.NOT_FOUND_BREED)

        val pet =
            Pet.create(
                userId = userId,
                name = command.name,
                profileImage = command.profileImage,
                relationship = command.relationship,
                relationshipText = command.relationshipText,
                breedId = command.breedId,
                gender = command.gender,
                birthYear = command.birthYear,
                weight = command.weight,
                isNeutered = command.isNeutered,
                isRepresentative = false,
            )

        val saved =
            try {
                savePetPort.registerWithinLimit(pet)
            } catch (e: IllegalStateException) {
                throw BusinessException(PetErrorCode.LIMIT_EXCEEDED)
            }

        return CreatePetResult(saved, breed)
    }

    private fun requireUserId(userCode: UserCode): Long {
        val user = loadUserPort.findByCode(userCode) ?: throw BusinessException(AuthErrorCode.NOT_FOUND_USER)
        return requireNotNull(user.id) { "저장되지 않은 User입니다." }.value
    }
}
