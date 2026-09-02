package com.petcampus.knockdog.domain.breed.application.service

import com.petcampus.knockdog.domain.breed.application.port.input.GetBreedsUseCase
import com.petcampus.knockdog.domain.breed.application.port.output.LoadBreedsPort
import com.petcampus.knockdog.domain.breed.domain.Breed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BreedQueryService(
    private val loadBreedsPort: LoadBreedsPort,
) : GetBreedsUseCase {
    @Transactional(readOnly = true)
    override fun getBreeds(query: String?): List<Breed> {
        val keyword = query.orEmpty().replace(WHITESPACE_REGEX, "")
        return if (keyword.isEmpty()) loadBreedsPort.findAllByDisplayOrder() else loadBreedsPort.search(keyword)
    }

    companion object {
        private val WHITESPACE_REGEX = Regex("\\s+")
    }
}
