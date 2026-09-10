package com.petcampus.knockdog.domain.kindergarten.adapter.outbound.user

import com.petcampus.knockdog.domain.auth.application.port.output.LoadUserPort
import com.petcampus.knockdog.domain.auth.domain.AddressType
import com.petcampus.knockdog.domain.auth.domain.UserCode
import com.petcampus.knockdog.domain.kindergarten.application.port.output.LoadComparisonAddressesPort
import com.petcampus.knockdog.domain.kindergarten.domain.ComparisonReferencePoint
import com.petcampus.knockdog.domain.kindergarten.domain.ComparisonReferencePointType
import org.springframework.stereotype.Component

@Component
class ComparisonAddressAdapter(
    private val loadUserPort: LoadUserPort,
) : LoadComparisonAddressesPort {
    override fun findByUserCode(userCode: String): List<ComparisonReferencePoint> {
        val user = loadUserPort.findByCode(UserCode(userCode)) ?: return emptyList()
        return user.addresses.map { address ->
            ComparisonReferencePoint(
                type = address.type.toComparisonReferencePointType(),
                lat = address.lat,
                lng = address.lng,
            )
        }
    }

    private fun AddressType.toComparisonReferencePointType(): ComparisonReferencePointType =
        when (this) {
            AddressType.HOME -> ComparisonReferencePointType.HOME
            AddressType.OTHER -> ComparisonReferencePointType.OTHER
        }
}
