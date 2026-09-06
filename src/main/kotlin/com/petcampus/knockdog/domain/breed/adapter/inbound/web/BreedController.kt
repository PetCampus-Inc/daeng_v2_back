package com.petcampus.knockdog.domain.breed.adapter.inbound.web

import com.petcampus.knockdog.domain.breed.application.port.input.GetBreedsUseCase
import com.petcampus.knockdog.domain.breed.domain.Breed
import com.petcampus.knockdog.global.response.Response
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/breeds")
class BreedController(
    private val getBreedsUseCase: GetBreedsUseCase,
) {
    @GetMapping
    fun getBreeds(
        @RequestParam(required = false) query: String?,
    ): ResponseEntity<Response<List<BreedResponse>>> =
        ResponseEntity.ok(Response.success(getBreedsUseCase.getBreeds(query).map(BreedResponse::from)))
}

data class BreedResponse(
    val id: Long,
    val nameKo: String,
    val alias: String?,
) {
    companion object {
        fun from(breed: Breed): BreedResponse = BreedResponse(breed.id, breed.nameKo, breed.alias)
    }
}
