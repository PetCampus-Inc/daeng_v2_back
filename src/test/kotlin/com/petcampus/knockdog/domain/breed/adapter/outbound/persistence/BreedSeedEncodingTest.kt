package com.petcampus.knockdog.domain.breed.adapter.outbound.persistence

import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BreedSeedEncodingTest {
    @Test
    fun `견종 시드는 UTF-8 특수문자를 보존한다`() {
        val seed =
            checkNotNull(javaClass.classLoader.getResourceAsStream("db/migration/V3__create_breeds.sql"))
                .use { String(it.readBytes(), StandardCharsets.UTF_8) }

        assertEquals(385, INSERT_ROW.findAll(seed).count())
        assertFalse(REPLACED_CHARACTER_IN_SQL_VALUE.containsMatchIn(seed))
        expectedEnglishNames.forEach { assertContains(seed, "'$it'") }
    }

    private companion object {
        val INSERT_ROW =
            Regex(
                """^  \(\d+, (?:NULL|\d+), '.*', '.*', (?:NULL|'.*'), CURRENT_TIMESTAMP\(6\), CURRENT_TIMESTAMP\(6\)\)(?:,|;)?$""",
                RegexOption.MULTILINE,
            )
        val REPLACED_CHARACTER_IN_SQL_VALUE = Regex("""'[^'\n]*\?[^'\n]*'""")
        val expectedEnglishNames =
            listOf(
                "SMÅLANDSSTÖVARE",
                "CIMARRÓN URUGUAYO",
                "SCHILLERSTÖVARE",
                "JÄMTHUND",
                "KROMFOHRLÄNDER",
                "KLEINER MÜNSTERLÄNDER",
                "PETIT BRABANÇON",
                "HAMILTONSTÖVARE",
            )
    }
}
