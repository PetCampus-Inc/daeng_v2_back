package com.petcampus.knockdog.domain.auth.adapter.outbound.oidc

import com.petcampus.knockdog.domain.auth.application.AuthErrorCode
import com.petcampus.knockdog.domain.auth.domain.Provider
import com.petcampus.knockdog.global.exception.BusinessException
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.util.concurrent.ConcurrentHashMap

/** provider별 JWKS를 프로세스 메모리에 캐시한다(레거시 `@Cacheable` 대응 — 별도 캐시 인프라 없이 단순화). */
@Component
class OidcPublicKeyClient(
    restClientBuilder: RestClient.Builder,
) {
    private val restClient = restClientBuilder.build()
    private val cache = ConcurrentHashMap<Provider, OidcPublicKeyList>()

    fun getPublicKeys(
        provider: Provider,
        url: String,
    ): OidcPublicKeyList = cache.getOrPut(provider) { fetch(url) }

    private fun fetch(url: String): OidcPublicKeyList =
        try {
            restClient
                .get()
                .uri(url)
                .retrieve()
                .body(OidcPublicKeyList::class.java)
                ?: throw BusinessException(AuthErrorCode.EXTERNAL_SERVER_ERROR)
        } catch (e: RestClientException) {
            throw BusinessException(AuthErrorCode.EXTERNAL_SERVER_ERROR)
        }
}
