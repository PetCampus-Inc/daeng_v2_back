package com.petcampus.knockdog.domain.breed.application.service

import com.petcampus.knockdog.domain.breed.application.port.output.LoadBreedsPort
import com.petcampus.knockdog.domain.breed.domain.Breed
import kotlin.test.Test
import kotlin.test.assertEquals

class BreedQueryServiceTest {
    @Test
    fun `공백 검색어는 표시 순서 목록을 조회한다`() {
        val expected = listOf(breed(1, "믹스견"))
        val port = FakeLoadBreedsPort(expected)

        val result = BreedQueryService(port).getBreeds("   ")

        assertEquals(expected, result)
        assertEquals(1, port.allCalls)
        assertEquals(emptyList(), port.queries)
    }

    @Test
    fun `검색어의 앞뒤 공백을 제거해 검색한다`() {
        val port = FakeLoadBreedsPort(emptyList())

        BreedQueryService(port).getBreeds("  휘펫 ")

        assertEquals(listOf("휘펫"), port.queries)
    }

    @Test
    fun `검색어 내부 공백도 제거해 검색한다`() {
        val port = FakeLoadBreedsPort(emptyList())

        BreedQueryService(port).getBreeds(" 골든 리트리버 ")

        assertEquals(listOf("골든리트리버"), port.queries)
    }

    private fun breed(
        id: Long,
        nameKo: String,
    ) = Breed(id, id.toInt(), null, "EN", nameKo, null)

    private class FakeLoadBreedsPort(
        private val all: List<Breed>,
    ) : LoadBreedsPort {
        var allCalls = 0
        val queries = mutableListOf<String>()

        override fun findAllByDisplayOrder(): List<Breed> {
            allCalls++
            return all
        }

        override fun search(query: String): List<Breed> {
            queries += query
            return emptyList()
        }

        override fun existsById(id: Long): Boolean = all.any { it.id == id }
    }
}
