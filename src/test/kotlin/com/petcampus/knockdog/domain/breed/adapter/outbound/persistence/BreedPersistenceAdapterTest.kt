package com.petcampus.knockdog.domain.breed.adapter.outbound.persistence

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals

@DataJpaTest
@Import(BreedPersistenceAdapter::class)
class BreedPersistenceAdapterTest(
    @Autowired private val breedJpaRepository: BreedJpaRepository,
    @Autowired private val breedPersistenceAdapter: BreedPersistenceAdapter,
) {
    @Test
    fun `검색어의 언더스코어를 LIKE 와일드카드가 아니라 리터럴로 취급한다`() {
        breedJpaRepository.saveAll(
            listOf(
                entity(1, "가나다"),
                entity(2, "가_다"),
            ),
        )

        val result = breedPersistenceAdapter.search("_")

        assertEquals(listOf("가_다"), result.map { it.nameKo })
    }

    @Test
    fun `검색어의 퍼센트를 LIKE 와일드카드가 아니라 리터럴로 취급한다`() {
        breedJpaRepository.saveAll(
            listOf(
                entity(1, "가나다"),
                entity(2, "가%다"),
            ),
        )

        val result = breedPersistenceAdapter.search("%")

        assertEquals(listOf("가%다"), result.map { it.nameKo })
    }

    @Test
    fun `검색어의 이스케이프 문자 자체도 리터럴로 취급한다`() {
        breedJpaRepository.saveAll(
            listOf(
                entity(1, "가나다"),
                entity(2, "가\\다"),
            ),
        )

        val result = breedPersistenceAdapter.search("\\")

        assertEquals(listOf("가\\다"), result.map { it.nameKo })
    }

    private fun entity(
        order: Int,
        nameKo: String,
        alias: String? = null,
    ) = BreedJpaEntity(
        displayOrder = order,
        fciStandardNumber = null,
        nameEn = "EN-$order",
        nameKo = nameKo,
        alias = alias,
    )
}
