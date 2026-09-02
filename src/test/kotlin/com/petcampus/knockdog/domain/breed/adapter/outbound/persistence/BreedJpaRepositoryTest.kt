package com.petcampus.knockdog.domain.breed.adapter.outbound.persistence

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import kotlin.test.assertEquals

@DataJpaTest
class BreedJpaRepositoryTest(
    @Autowired private val breedJpaRepository: BreedJpaRepository,
) {
    @Test
    fun `검색은 시작 일치 후 포함 일치 순으로 한글명 정렬한다`() {
        breedJpaRepository.saveAll(
            listOf(
                entity(1, "바가"),
                entity(2, "가나다"),
                entity(3, "가라", "다른 별칭"),
                entity(4, "나다", "가별칭"),
            ),
        )

        val result = breedJpaRepository.search("가")

        assertEquals(listOf("가나다", "가라", "나다", "바가"), result.map { it.nameKo })
    }

    @Test
    fun `이름에 포함된 공백은 무시하고 검색한다`() {
        breedJpaRepository.saveAll(
            listOf(
                entity(1, "골든 리트리버"),
                entity(2, "래브라도 리트리버", "래브라도"),
            ),
        )

        val result = breedJpaRepository.search("골든리트리버")

        assertEquals(listOf("골든 리트리버"), result.map { it.nameKo })
    }

    @Test
    fun `전체 목록은 표시 순서로 정렬한다`() {
        breedJpaRepository.saveAll(
            listOf(
                entity(2, "나"),
                entity(1, "다"),
            ),
        )

        assertEquals(listOf(1, 2), breedJpaRepository.findAllByOrderByDisplayOrderAsc().map { it.displayOrder })
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
