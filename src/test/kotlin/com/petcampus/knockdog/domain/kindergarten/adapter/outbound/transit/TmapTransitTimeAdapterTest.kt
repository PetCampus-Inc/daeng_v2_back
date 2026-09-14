package com.petcampus.knockdog.domain.kindergarten.adapter.outbound.transit

import com.petcampus.knockdog.domain.kindergarten.domain.TransportationType
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TmapTransitTimeAdapterTest {
    private val properties = TmapProperties(key = "test-key", baseUrl = "http://tmap.test", cacheTtlDays = 7)

    @Suppress("UNCHECKED_CAST")
    private fun redisTemplate(cached: String? = null): StringRedisTemplate {
        val valueOperations = Mockito.mock(ValueOperations::class.java) as ValueOperations<String, String>
        Mockito.`when`(valueOperations.get(Mockito.anyString())).thenReturn(cached)
        val template = Mockito.mock(StringRedisTemplate::class.java)
        Mockito.`when`(template.opsForValue()).thenReturn(valueOperations)
        return template
    }

    @Suppress("UNCHECKED_CAST")
    private fun failingRedisTemplate(): StringRedisTemplate {
        val valueOperations = Mockito.mock(ValueOperations::class.java) as ValueOperations<String, String>
        Mockito
            .`when`(valueOperations.get(Mockito.anyString()))
            .thenThrow(RuntimeException("redis down"))
        Mockito
            .doThrow(RuntimeException("redis down"))
            .`when`(valueOperations)
            .set(Mockito.anyString(), Mockito.anyString(), Mockito.any(Duration::class.java))
        val template = Mockito.mock(StringRedisTemplate::class.java)
        Mockito.`when`(template.opsForValue()).thenReturn(valueOperations)
        return template
    }

    private fun adapterWithServer(redisTemplate: StringRedisTemplate): Pair<TmapTransitTimeAdapter, MockRestServiceServer> {
        val builder = RestClient.builder()
        // 도보·자동차·대중교통 3건이 병렬로 호출되므로 도착 순서를 강제하지 않는다.
        val server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build()
        return TmapTransitTimeAdapter(builder, properties, redisTemplate) to server
    }

    @Test
    fun `캐시가 비어 있으면 세 교통수단을 모두 TMAP에서 조회하고 캐시에 저장한다`() {
        val redisTemplate = redisTemplate(cached = null)
        val (adapter, server) = adapterWithServer(redisTemplate)

        server
            .expect(requestTo("http://tmap.test/tmap/routes/pedestrian?version=1"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("""{"features":[{"properties":{"totalTime":300}}]}""", MediaType.APPLICATION_JSON))
        server
            .expect(requestTo("http://tmap.test/tmap/routes?version=1"))
            .andRespond(withSuccess("""{"features":[{"properties":{"totalTime":600}}]}""", MediaType.APPLICATION_JSON))
        server
            .expect(requestTo("http://tmap.test/transit/routes"))
            .andRespond(
                withSuccess(
                    """{"metaData":{"plan":{"itineraries":[{"totalTime":900}]}}}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val result = adapter.findTransitTimes(37.5, 127.0, 37.6, 127.1)

        assertEquals(300, result.single { it.type == TransportationType.WALKING }.seconds)
        assertEquals(600, result.single { it.type == TransportationType.DRIVING }.seconds)
        assertEquals(900, result.single { it.type == TransportationType.TRANSIT }.seconds)
        server.verify()
        Mockito
            .verify(redisTemplate.opsForValue(), Mockito.times(3))
            .set(Mockito.anyString(), Mockito.anyString(), Mockito.eq(Duration.ofDays(7)))
    }

    @Test
    fun `캐시에 값이 있으면 TMAP을 호출하지 않는다`() {
        val redisTemplate = redisTemplate(cached = "150")
        val (adapter, server) = adapterWithServer(redisTemplate)

        val result = adapter.findTransitTimes(37.5, 127.0, 37.6, 127.1)

        assertEquals(listOf(150, 150, 150), result.map { it.seconds })
        server.verify()
    }

    @Test
    fun `TMAP 호출이 실패한 교통수단만 time이 null이다`() {
        val redisTemplate = redisTemplate(cached = null)
        val (adapter, server) = adapterWithServer(redisTemplate)

        server
            .expect(requestTo("http://tmap.test/tmap/routes/pedestrian?version=1"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))
        server
            .expect(requestTo("http://tmap.test/tmap/routes?version=1"))
            .andRespond(withSuccess("""{"features":[{"properties":{"totalTime":600}}]}""", MediaType.APPLICATION_JSON))
        server
            .expect(requestTo("http://tmap.test/transit/routes"))
            .andRespond(
                withSuccess(
                    """{"metaData":{"plan":{"itineraries":[{"totalTime":900}]}}}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val result = adapter.findTransitTimes(37.5, 127.0, 37.6, 127.1)

        assertNull(result.single { it.type == TransportationType.WALKING }.seconds)
        assertEquals(600, result.single { it.type == TransportationType.DRIVING }.seconds)
        assertEquals(900, result.single { it.type == TransportationType.TRANSIT }.seconds)
    }

    @Test
    fun `경로가 없으면(204) time은 null이다`() {
        val redisTemplate = redisTemplate(cached = null)
        val (adapter, server) = adapterWithServer(redisTemplate)

        server
            .expect(requestTo("http://tmap.test/tmap/routes/pedestrian?version=1"))
            .andRespond(withNoContent())
        server
            .expect(requestTo("http://tmap.test/tmap/routes?version=1"))
            .andRespond(withNoContent())
        server
            .expect(requestTo("http://tmap.test/transit/routes"))
            .andRespond(withNoContent())

        val result = adapter.findTransitTimes(37.5, 127.0, 37.6, 127.1)

        assertEquals(listOf(null, null, null), result.map { it.seconds })
    }

    @Test
    fun `Redis 캐시 조회·저장이 실패해도 TMAP 결과는 정상 반환한다`() {
        val redisTemplate = failingRedisTemplate()
        val (adapter, server) = adapterWithServer(redisTemplate)

        server
            .expect(requestTo("http://tmap.test/tmap/routes/pedestrian?version=1"))
            .andRespond(withSuccess("""{"features":[{"properties":{"totalTime":300}}]}""", MediaType.APPLICATION_JSON))
        server
            .expect(requestTo("http://tmap.test/tmap/routes?version=1"))
            .andRespond(withSuccess("""{"features":[{"properties":{"totalTime":600}}]}""", MediaType.APPLICATION_JSON))
        server
            .expect(requestTo("http://tmap.test/transit/routes"))
            .andRespond(
                withSuccess(
                    """{"metaData":{"plan":{"itineraries":[{"totalTime":900}]}}}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val result = adapter.findTransitTimes(37.5, 127.0, 37.6, 127.1)

        assertEquals(300, result.single { it.type == TransportationType.WALKING }.seconds)
        assertEquals(600, result.single { it.type == TransportationType.DRIVING }.seconds)
        assertEquals(900, result.single { it.type == TransportationType.TRANSIT }.seconds)
    }
}
