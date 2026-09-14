package com.petcampus.knockdog.domain.kindergarten.application.port.output

import com.petcampus.knockdog.domain.kindergarten.domain.TransitTime

interface LoadTransitTimesPort {
    fun findTransitTimes(
        originLat: Double,
        originLng: Double,
        destinationLat: Double,
        destinationLng: Double,
    ): List<TransitTime>
}
