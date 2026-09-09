package com.petcampus.knockdog.domain.pet.application.service

import com.petcampus.knockdog.domain.auth.application.service.RequireUserId
import com.petcampus.knockdog.domain.pet.application.port.input.GetPetsCommand
import com.petcampus.knockdog.domain.pet.application.port.input.GetPetsResult
import com.petcampus.knockdog.domain.pet.application.port.input.GetPetsUseCase
import com.petcampus.knockdog.domain.pet.application.port.input.PetWithBreed
import com.petcampus.knockdog.domain.pet.application.port.output.LoadBreedPort
import com.petcampus.knockdog.domain.pet.application.port.output.LoadPetPort
import com.petcampus.knockdog.domain.pet.domain.Pet
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetPetsService(
    private val requireUserId: RequireUserId,
    private val loadPetPort: LoadPetPort,
    private val loadBreedPort: LoadBreedPort,
) : GetPetsUseCase {
    @Transactional(readOnly = true)
    override fun getPets(command: GetPetsCommand): GetPetsResult {
        val userId = requireUserId(command.userCode)

        val pets =
            loadPetPort
                .findAllActiveByUserId(userId)
                .sortedWith(compareBy<Pet> { !it.isRepresentative }.thenBy { it.name })

        val petsWithBreed =
            pets.map { pet ->
                val breed =
                    checkNotNull(loadBreedPort.findById(pet.breedId)) {
                        "pet(${pet.id?.value})이 참조하는 breed(${pet.breedId})가 존재하지 않습니다."
                    }
                PetWithBreed(pet, breed)
            }

        return GetPetsResult(petsWithBreed)
    }
}
