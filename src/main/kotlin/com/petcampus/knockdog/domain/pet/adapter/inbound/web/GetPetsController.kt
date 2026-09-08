package com.petcampus.knockdog.domain.pet.adapter.inbound.web

import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.pet.application.port.input.GetPetsCommand
import com.petcampus.knockdog.domain.pet.application.port.input.GetPetsUseCase
import com.petcampus.knockdog.global.response.Response
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/pets")
class GetPetsController(
    private val getPetsUseCase: GetPetsUseCase,
) {
    @GetMapping
    fun getPets(
        @AuthenticationPrincipal userCode: String,
    ): ResponseEntity<Response<List<PetResponse>>> {
        val result = getPetsUseCase.getPets(GetPetsCommand(userCode = UserCode(userCode)))

        return ResponseEntity.ok(
            Response.success(result.pets.map { PetResponse.of(it.pet, it.breed) }),
        )
    }
}
