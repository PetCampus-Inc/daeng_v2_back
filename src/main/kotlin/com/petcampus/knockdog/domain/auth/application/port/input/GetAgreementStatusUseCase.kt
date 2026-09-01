package com.petcampus.knockdog.domain.auth.application.port.input

import com.petcampus.knockdog.domain.auth.domain.UserCode

interface GetAgreementStatusUseCase {
    fun hasAgreedRequiredTerms(userCode: UserCode): Boolean
}
