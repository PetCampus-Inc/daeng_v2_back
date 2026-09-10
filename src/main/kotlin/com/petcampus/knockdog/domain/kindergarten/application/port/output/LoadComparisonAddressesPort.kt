package com.petcampus.knockdog.domain.kindergarten.application.port.output

import com.petcampus.knockdog.domain.kindergarten.domain.ComparisonReferencePoint

interface LoadComparisonAddressesPort {
    fun findByUserCode(userCode: String): List<ComparisonReferencePoint>
}
