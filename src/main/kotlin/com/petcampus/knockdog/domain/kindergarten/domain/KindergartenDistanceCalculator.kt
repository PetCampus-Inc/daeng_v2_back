package com.petcampus.knockdog.domain.kindergarten.domain

import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

/**
 * 사용자 좌표와 유치원 좌표 사이 직선거리(km)를 계산한다.
 * 레거시 `KindergartenQueryService.calculateDistance`와 동일한 구면 삼각법 근사식을 그대로 이식했다 —
 * 정밀한 Haversine은 아니지만(근거리에서 acos 정밀도 이슈가 있을 수 있음) 레거시와 응답을 맞추기 위해 그대로 둔다.
 */
object KindergartenDistanceCalculator {
    private const val NAUTICAL_TO_STATUTE_MILE = 1.1515
    private const val MILE_TO_KM = 1.609344

    fun calculateKm(
        userLat: Double,
        userLng: Double,
        targetLat: Double,
        targetLng: Double,
    ): Double {
        if (userLat == targetLat && userLng == targetLng) return 0.0

        val theta = userLng - targetLng
        var dist =
            sin(deg2rad(userLat)) * sin(deg2rad(targetLat)) +
                cos(deg2rad(userLat)) * cos(deg2rad(targetLat)) * cos(deg2rad(theta))
        dist = acos(dist.coerceIn(-1.0, 1.0))
        dist = rad2deg(dist)
        dist *= 60 * NAUTICAL_TO_STATUTE_MILE
        dist *= MILE_TO_KM
        return dist
    }

    private fun deg2rad(deg: Double): Double = deg * Math.PI / 180.0

    private fun rad2deg(rad: Double): Double = rad * 180 / Math.PI
}
