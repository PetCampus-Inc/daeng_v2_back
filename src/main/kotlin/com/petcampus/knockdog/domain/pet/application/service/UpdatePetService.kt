package com.petcampus.knockdog.domain.pet.application.service

import com.petcampus.knockdog.domain.auth.application.AuthErrorCode
import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserPort
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.auth.domain.UserId
import com.petcampus.knockdog.domain.pet.application.PetErrorCode
import com.petcampus.knockdog.domain.pet.application.port.input.UpdatePetCommand
import com.petcampus.knockdog.domain.pet.application.port.input.UpdatePetResult
import com.petcampus.knockdog.domain.pet.application.port.input.UpdatePetUseCase
import com.petcampus.knockdog.domain.pet.application.port.output.LoadBreedPort
import com.petcampus.knockdog.domain.pet.application.port.output.LoadPetPort
import com.petcampus.knockdog.domain.pet.application.port.output.SavePetPort
import com.petcampus.knockdog.domain.pet.domain.Relationship
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdatePetService(
    private val loadUserPort: LoadUserPort,
    private val loadPetPort: LoadPetPort,
    private val loadBreedPort: LoadBreedPort,
    private val savePetPort: SavePetPort,
) : UpdatePetUseCase {
    @Transactional
    override fun update(command: UpdatePetCommand): UpdatePetResult {
        val userId = requireUserId(command.userCode)
        val pet =
            loadPetPort.findById(command.petId)?.takeIf { !it.isDeleted }
                ?: throw BusinessException(PetErrorCode.NOT_FOUND)
        if (pet.userId != userId) throw BusinessException(PetErrorCode.NOT_AUTHORIZED)

        val effectiveName = command.name.orElse(pet.name)
        requireNotNull(effectiveName) { "name은 null일 수 없습니다." }

        val effectiveRelationship = command.relationship.orElse(pet.relationship)
        requireNotNull(effectiveRelationship) { "relationship은 null일 수 없습니다." }

        val effectiveRelationshipText =
            if (command.relationshipText.isPresent) {
                command.relationshipText.get()
            } else if (effectiveRelationship != Relationship.ETC) {
                null
            } else {
                pet.relationshipText
            }

        val effectiveBreedId = command.breedId.orElse(pet.breedId)
        requireNotNull(effectiveBreedId) { "breedId는 null일 수 없습니다." }
        val breed = loadBreedPort.findById(effectiveBreedId) ?: throw BusinessException(PetErrorCode.NOT_FOUND_BREED)

        val effectiveGender = command.gender.orElse(pet.gender)
        requireNotNull(effectiveGender) { "gender는 null일 수 없습니다." }

        val effectiveWeight = command.weight.orElse(pet.weight)
        requireNotNull(effectiveWeight) { "weight는 null일 수 없습니다." }

        pet.update(
            name = effectiveName,
            profileImage = command.profileImage.orElse(pet.profileImage),
            relationship = effectiveRelationship,
            relationshipText = effectiveRelationshipText,
            breedId = effectiveBreedId,
            gender = effectiveGender,
            birthYear = command.birthYear.orElse(pet.birthYear),
            weight = effectiveWeight,
            isNeutered = command.isNeutered.orElse(pet.isNeutered),
        )

        return UpdatePetResult(savePetPort.save(pet), breed)
    }

    private fun requireUserId(userCode: UserCode): UserId {
        val user = loadUserPort.findByCode(userCode) ?: throw BusinessException(AuthErrorCode.NOT_FOUND_USER)
        return requireNotNull(user.id) { "저장되지 않은 User입니다." }
    }
}
