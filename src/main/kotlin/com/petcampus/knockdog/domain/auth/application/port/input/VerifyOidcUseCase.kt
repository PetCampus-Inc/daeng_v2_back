package com.petcampus.knockdog.domain.auth.application.port.input

import com.petcampus.knockdog.domain.auth.domain.Provider
import com.petcampus.knockdog.domain.auth.domain.SocialUser

interface VerifyOidcUseCase {
    fun verify(command: VerifyOidcCommand): VerifyOidcResult
}

data class VerifyOidcCommand(
    val provider: Provider,
    val idToken: String,
    val name: String?,
    val picture: String?,
)

data class VerifyOidcResult(
    val socialUser: SocialUser,
    val oidcToken: String,
)
