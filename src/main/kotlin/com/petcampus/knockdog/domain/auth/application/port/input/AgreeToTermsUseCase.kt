package com.petcampus.knockdog.domain.auth.application.port.input

import com.petcampus.knockdog.domain.auth.domain.AgreementTermType
import com.petcampus.knockdog.domain.auth.domain.UserCode

interface AgreeToTermsUseCase {
    fun agree(command: AgreeToTermsCommand)
}

data class AgreeToTermsCommand(
    val userCode: UserCode,
    val agreedTerms: List<AgreementTermType>,
)
