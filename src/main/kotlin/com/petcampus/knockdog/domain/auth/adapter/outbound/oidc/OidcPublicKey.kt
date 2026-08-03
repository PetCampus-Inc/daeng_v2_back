package com.petcampus.knockdog.domain.auth.adapter.outbound.oidc

import com.petcampus.knockdog.domain.auth.application.AuthException

data class OidcPublicKey(
    val kid: String,
    val kty: String,
    val alg: String? = null,
    val use: String? = null,
    val n: String,
    val e: String,
)

data class OidcPublicKeyList(
    val keys: List<OidcPublicKey>,
) {
    fun getMatchedKey(
        kid: String?,
        alg: String?,
    ): OidcPublicKey =
        keys.firstOrNull { it.kid == kid && (it.alg == null || it.alg == alg) }
            ?: throw AuthException.tokenVerificationFailed()
}
