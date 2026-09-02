package com.petcampus.knockdog.domain.kindergarten.adapter.outbound.seed

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.petcampus.knockdog.domain.kindergarten.application.port.output.SaveKindergartenPort
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource

@Configuration
class KindergartenJsonSeederConfig {
    @Bean
    fun kindergartenJsonSeeder(
        saveKindergartenPort: SaveKindergartenPort,
        properties: KindergartenSeedProperties,
    ) = ApplicationRunner {
        if (properties.enabled) KindergartenJsonSeeder(saveKindergartenPort).seed()
    }
}

@ConfigurationProperties(prefix = "kindergarten.seed")
data class KindergartenSeedProperties(
    val enabled: Boolean = false,
)

class KindergartenJsonSeeder(
    private val saveKindergartenPort: SaveKindergartenPort,
) {
    private val objectMapper: ObjectMapper = buildObjectMapper()

    fun seed() {
        val kindergartens: List<CrawledKindergarten> = readResource("kindergarten/info_new.json")
        val menus: List<CrawledMenu> = readResource("kindergarten/price_and_product.json")
        val menusByKindergartenId = menus.groupBy { it.kindergartenId }

        var seeded = 0
        var skipped = 0
        for (crawled in kindergartens) {
            val naverPlaceId = crawled.id.toString()
            if (saveKindergartenPort.existsByNaverPlaceId(naverPlaceId)) {
                skipped++
                continue
            }
            val domain = KindergartenSeedConverter.toDomain(crawled, menusByKindergartenId[crawled.id] ?: emptyList())
            saveKindergartenPort.save(domain)
            seeded++
        }
        log.info("유치원 시딩 완료 — 신규 {}건, 건너뜀(이미 존재) {}건", seeded, skipped)
    }

    private inline fun <reified T> readResource(path: String): List<T> =
        ClassPathResource(path).inputStream.use { objectMapper.readValue(it) }

    companion object {
        private val log = LoggerFactory.getLogger(KindergartenJsonSeeder::class.java)

        fun buildObjectMapper(): ObjectMapper =
            jacksonObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
}
