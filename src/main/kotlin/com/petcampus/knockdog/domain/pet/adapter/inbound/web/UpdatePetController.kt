package com.petcampus.knockdog.domain.pet.adapter.inbound.web

import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.pet.application.port.input.UpdatePetCommand
import com.petcampus.knockdog.domain.pet.application.port.input.UpdatePetUseCase
import com.petcampus.knockdog.domain.pet.domain.Gender
import com.petcampus.knockdog.domain.pet.domain.PetId
import com.petcampus.knockdog.domain.pet.domain.Relationship
import com.petcampus.knockdog.global.response.Response
import org.openapitools.jackson.nullable.JsonNullable
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/pets")
class UpdatePetController(
    private val updatePetUseCase: UpdatePetUseCase,
) {
    @PatchMapping("/{petId}")
    fun update(
        @AuthenticationPrincipal userCode: String,
        @PathVariable petId: Long,
        @RequestBody request: UpdatePetRequest,
    ): ResponseEntity<Response<PetResponse>> {
        val result =
            updatePetUseCase.update(
                UpdatePetCommand(
                    userCode = UserCode(userCode),
                    petId = PetId(petId),
                    name = request.name,
                    profileImage = request.profileImage,
                    relationship = request.relationship,
                    relationshipText = request.relationshipText,
                    breedId = request.breedId,
                    gender = request.gender,
                    birthYear = request.birthYear,
                    weight = request.weight,
                    isNeutered = request.isNeutered,
                ),
            )

        return ResponseEntity.ok(Response.success(PetResponse.of(result.pet, result.breed)))
    }
}

data class UpdatePetRequest(
    val name: JsonNullable<String> = JsonNullable.undefined(),
    val profileImage: JsonNullable<String?> = JsonNullable.undefined(),
    val relationship: JsonNullable<Relationship> = JsonNullable.undefined(),
    val relationshipText: JsonNullable<String?> = JsonNullable.undefined(),
    val breedId: JsonNullable<Long> = JsonNullable.undefined(),
    val gender: JsonNullable<Gender> = JsonNullable.undefined(),
    val birthYear: JsonNullable<Int?> = JsonNullable.undefined(),
    val weight: JsonNullable<Double> = JsonNullable.undefined(),
    val isNeutered: JsonNullable<Boolean?> = JsonNullable.undefined(),
)
