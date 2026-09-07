package com.petcampus.knockdog.domain.pet.adapter.inbound.web

import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.pet.application.port.input.SetRepresentativeCommand
import com.petcampus.knockdog.domain.pet.application.port.input.SetRepresentativeUseCase
import com.petcampus.knockdog.domain.pet.domain.PetId
import com.petcampus.knockdog.global.response.Response
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/pets")
class SetRepresentativeController(
    private val setRepresentativeUseCase: SetRepresentativeUseCase,
) {
    @PutMapping("/{petId}/representative")
    fun setRepresentative(
        @AuthenticationPrincipal userCode: String,
        @PathVariable petId: Long,
    ): ResponseEntity<Response<PetResponse>> {
        val result =
            setRepresentativeUseCase.setRepresentative(
                SetRepresentativeCommand(userCode = UserCode(userCode), petId = PetId(petId)),
            )

        return ResponseEntity.ok(Response.success(PetResponse.of(result.pet, result.breed)))
    }
}
