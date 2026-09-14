package com.petcampus.knockdog.domain.kindergarten.adapter.outbound.transit

import com.fasterxml.jackson.databind.JsonNode
import com.petcampus.knockdog.domain.kindergarten.application.port.output.LoadTransitTimesPort
import com.petcampus.knockdog.domain.kindergarten.domain.TransitTime
import com.petcampus.knockdog.domain.kindergarten.domain.TransportationType
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Duration
import java.util.concurrent.CompletableFuture
import kotlin.math.roundToLong

@Component
class TmapTransitTimeAdapter(
    restClientBuilder: RestClient.Builder,
    properties: TmapProperties,
    private val redisTemplate: StringRedisTemplate,
) : LoadTransitTimesPort {
    private val cacheTtl = Duration.ofDays(properties.cacheTtlDays)

    private val restClient =
        restClientBuilder
            .baseUrl(properties.baseUrl)
            .defaultHeader("appKey", properties.key)
            .build()

    override fun findTransitTimes(
        originLat: Double,
        originLng: Double,
        destinationLat: Double,
        destinationLng: Double,
    ): List<TransitTime> =
        TransportationType.entries
            .map { type ->
                type to
                    CompletableFuture.supplyAsync {
                        secondsOf(type, originLat, originLng, destinationLat, destinationLng)
                    }
            }.map { (type, future) -> TransitTime(type, future.join()) }

    private fun secondsOf(
        type: TransportationType,
        originLat: Double,
        originLng: Double,
        destinationLat: Double,
        destinationLng: Double,
    ): Int? {
        val cacheKey = cacheKeyOf(type, originLat, originLng, destinationLat, destinationLng)
        redisTemplate
            .opsForValue()
            .get(cacheKey)
            ?.toIntOrNull()
            ?.let { return it }

        val seconds = fetch(type, originLat, originLng, destinationLat, destinationLng) ?: return null
        redisTemplate.opsForValue().set(cacheKey, seconds.toString(), cacheTtl)
        return seconds
    }

    private fun fetch(
        type: TransportationType,
        originLat: Double,
        originLng: Double,
        destinationLat: Double,
        destinationLng: Double,
    ): Int? =
        try {
            when (type) {
                TransportationType.WALKING ->
                    routeSeconds(PEDESTRIAN_PATH, originLat, originLng, destinationLat, destinationLng, includeName = true)
                TransportationType.DRIVING ->
                    routeSeconds(CAR_PATH, originLat, originLng, destinationLat, destinationLng, includeName = false)
                TransportationType.TRANSIT ->
                    transitSeconds(originLat, originLng, destinationLat, destinationLng)
            }
        } catch (e: Exception) {
            log.warn("TMAP {} 이동시간 조회 실패: {}", type, e.message)
            null
        }

    private fun routeSeconds(
        path: String,
        originLat: Double,
        originLng: Double,
        destinationLat: Double,
        destinationLng: Double,
        includeName: Boolean,
    ): Int? {
        val body =
            buildMap<String, Any> {
                put("startX", originLng.toString())
                put("startY", originLat.toString())
                put("endX", destinationLng.toString())
                put("endY", destinationLat.toString())
                if (includeName) {
                    put("startName", "출발지")
                    put("endName", "도착지")
                }
            }

        val response =
            restClient
                .post()
                .uri(path)
                .body(body)
                .retrieve()
                .body(JsonNode::class.java) ?: return null

        return response
            .path("features")
            .firstNotNullOfOrNull { it.path("properties").totalTimeOrNull() }
    }

    private fun transitSeconds(
        originLat: Double,
        originLng: Double,
        destinationLat: Double,
        destinationLng: Double,
    ): Int? {
        val body =
            mapOf(
                "startX" to originLng.toString(),
                "startY" to originLat.toString(),
                "endX" to destinationLng.toString(),
                "endY" to destinationLat.toString(),
                "count" to 1,
                "lang" to 0,
                "format" to "json",
            )

        val response =
            restClient
                .post()
                .uri(TRANSIT_PATH)
                .body(body)
                .retrieve()
                .body(JsonNode::class.java) ?: return null

        return response
            .path("metaData")
            .path("plan")
            .path("itineraries")
            .firstNotNullOfOrNull { it.totalTimeOrNull() }
    }

    private fun JsonNode.totalTimeOrNull(): Int? {
        val totalTime = path("totalTime")
        return if (totalTime.isMissingNode || !totalTime.isNumber) null else totalTime.asInt()
    }

    private fun cacheKeyOf(
        type: TransportationType,
        originLat: Double,
        originLng: Double,
        destinationLat: Double,
        destinationLng: Double,
    ): String = "transit:$type:${gridHash(originLat, originLng)}:${gridHash(destinationLat, destinationLng)}"

    private fun gridHash(
        lat: Double,
        lng: Double,
    ): String = "${(lat * GRID_PRECISION).roundToLong()}_${(lng * GRID_PRECISION).roundToLong()}"

    companion object {
        private val log = LoggerFactory.getLogger(TmapTransitTimeAdapter::class.java)
        private const val PEDESTRIAN_PATH = "/tmap/routes/pedestrian?version=1"
        private const val CAR_PATH = "/tmap/routes?version=1"
        private const val TRANSIT_PATH = "/transit/routes"
        private const val GRID_PRECISION = 1000
    }
}
