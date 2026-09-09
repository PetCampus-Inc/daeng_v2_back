package com.petcampus.knockdog.domain.pet.adapter.inbound.web

import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.pet.application.port.input.DeletePetCommand
import com.petcampus.knockdog.domain.pet.application.port.input.DeletePetUseCase
import com.petcampus.knockdog.domain.pet.domain.PetId
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/pets")
class DeletePetController(
    private val deletePetUseCase: DeletePetUseCase,
) {
    @DeleteMapping("/{petId}")
    fun delete(
        @AuthenticationPrincipal userCode: String,
        @PathVariable petId: Long,
    ): ResponseEntity<Void> {
        deletePetUseCase.delete(DeletePetCommand(userCode = UserCode(userCode), petId = PetId(petId)))
        return ResponseEntity.noContent().build()
    }
}
