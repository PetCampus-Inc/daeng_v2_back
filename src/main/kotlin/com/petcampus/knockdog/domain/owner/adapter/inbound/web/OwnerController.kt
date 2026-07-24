package com.petcampus.knockdog.domain.owner.adapter.inbound.web

import com.petcampus.knockdog.domain.owner.application.port.input.GetOwnerUseCase
import com.petcampus.knockdog.domain.owner.application.port.input.RegisterOwnerCommand
import com.petcampus.knockdog.domain.owner.application.port.input.RegisterOwnerUseCase
import com.petcampus.knockdog.domain.owner.domain.Owner
import com.petcampus.knockdog.domain.owner.domain.OwnerId
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/owners")
class OwnerController(
    private val registerOwnerUseCase: RegisterOwnerUseCase,
    private val getOwnerUseCase: GetOwnerUseCase,
) {
    @PostMapping
    fun register(
        @RequestBody request: RegisterOwnerRequest,
    ): ResponseEntity<OwnerResponse> {
        val owner = registerOwnerUseCase.register(RegisterOwnerCommand(request.email))
        return ResponseEntity.status(HttpStatus.CREATED).body(OwnerResponse.from(owner))
    }

    @GetMapping("/{id}")
    fun getOne(
        @PathVariable id: String,
    ): OwnerResponse = OwnerResponse.from(getOwnerUseCase.getById(OwnerId(id)))
}

data class RegisterOwnerRequest(
    val email: String,
)

data class OwnerResponse(
    val id: String,
    val email: String,
    val status: String,
) {
    companion object {
        fun from(owner: Owner): OwnerResponse = OwnerResponse(owner.id.value, owner.email.value, owner.status.name)
    }
}
