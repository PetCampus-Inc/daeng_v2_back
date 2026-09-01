package com.petcampus.knockdog.domain.auth.application.port.output

import com.petcampus.knockdog.domain.auth.domain.UserAgreement

interface SaveUserAgreementPort {
    fun saveAll(agreements: List<UserAgreement>)
}
