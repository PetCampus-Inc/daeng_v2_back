package com.petcampus.knockdog.domain.kindergarten.domain

import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.test.assertTrue

class KindergartenDistanceCalculatorTest {
    @Test
    fun `같은 좌표면 거리는 0이다`() {
        val distance = KindergartenDistanceCalculator.calculateKm(37.5, 127.0, 37.5, 127.0)

        assertTrue(abs(distance) < 0.0001, "계산된 거리: $distance")
    }

    @Test
    fun `서울시청과 강남역 거리는 대략 8km대다`() {
        // 서울시청(37.5663, 126.9779) ~ 강남역(37.4979, 127.0276) 실측 직선거리 약 8.4km
        val distance = KindergartenDistanceCalculator.calculateKm(37.5663, 126.9779, 37.4979, 127.0276)

        assertTrue(abs(distance - 8.4) < 1.0, "계산된 거리: $distance")
    }
}
