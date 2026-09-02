package com.petcampus.knockdog.domain.kindergarten.adapter.outbound.seed

import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class KindergartenJsonSeederTest {
    @Test
    fun `크롤링 JSON에 매핑하지 않은 필드가 있어도 파싱에 실패하지 않는다`() {
        val objectMapper = KindergartenJsonSeeder.buildObjectMapper()

        val json =
            """
            [{"id": 1, "name": "테스트", "address": "서울시", "road_address": "서울시 도로명", "business_services": ["KINDERGARTEN"], "unmapped_field": "x"}]
            """.trimIndent()

        val result: List<CrawledKindergarten> = objectMapper.readValue(json)

        assertEquals(1, result.size)
        assertEquals("테스트", result.single().name)
    }
}
