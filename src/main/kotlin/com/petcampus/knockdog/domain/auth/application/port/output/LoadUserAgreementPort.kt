package com.petcampus.knockdog.domain.auth.application.port.output

import com.petcampus.knockdog.domain.auth.domain.AgreementTermType
import com.petcampus.knockdog.domain.auth.domain.UserId

interface LoadUserAgreementPort {
    fun findTermTypesByUserId(userId: UserId): Set<AgreementTermType>
}
