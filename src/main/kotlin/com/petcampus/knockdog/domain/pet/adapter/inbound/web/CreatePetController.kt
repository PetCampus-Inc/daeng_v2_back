package com.petcampus.knockdog.domain.pet.adapter.inbound.web

import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.pet.application.port.input.CreatePetCommand
import com.petcampus.knockdog.domain.pet.application.port.input.CreatePetUseCase
import com.petcampus.knockdog.domain.pet.domain.Gender
import com.petcampus.knockdog.domain.pet.domain.Relationship
import com.petcampus.knockdog.global.response.Response
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/pets")
class CreatePetController(
    private val createPetUseCase: CreatePetUseCase,
) {
    @PostMapping
    fun create(
        @AuthenticationPrincipal userCode: String,
        @RequestBody request: CreatePetRequest,
    ): ResponseEntity<Response<PetResponse>> {
        val result =
            createPetUseCase.create(
                CreatePetCommand(
                    userCode = UserCode(userCode),
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

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(Response.success(PetResponse.of(result.pet, result.breed)))
    }
}

data class CreatePetRequest(
    val name: String,
    val profileImage: String?,
    val relationship: Relationship,
    val relationshipText: String?,
    val breedId: Long,
    val gender: Gender,
    val birthYear: Int?,
    val weight: Double,
    val isNeutered: Boolean?,
)
