package com.petcampus.knockdog.domain.auth.adapter.outbound.oidc

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.Date
import kotlin.test.assertEquals

/**
 * JWKS(n, e)로부터 PublicKey를 복원해 서명 검증까지 이어지는 경로를 검증한다.
 * 네트워크(OidcPublicKeyClient) 없이, 자체 발급한 RSA 키쌍으로 JWKS 응답을 흉내낸다.
 */
class OidcTokenVerificationTest {
    private val publicKeyFactory = OidcPublicKeyFactory()
    private val tokenParser = OidcTokenParser()
    private val headerParser = JwtHeaderParser(ObjectMapper())

    @Test
    fun `JWKS의 n,e로 만든 PublicKey로 서명된 ID Token을 검증하고 클레임을 읽을 수 있다`() {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val publicKey = keyPair.public as RSAPublicKey
        val privateKey = keyPair.private as RSAPrivateKey

        val jwk =
            OidcPublicKey(
                kid = "test-kid",
                kty = "RSA",
                alg = "RS256",
                n = publicKey.modulus.toBase64Url(),
                e = publicKey.publicExponent.toBase64Url(),
            )

        val idToken =
            Jwts
                .builder()
                .header()
                .keyId("test-kid")
                .and()
                .subject("provider-user-1")
                .claim("email", "a@b.com")
                .issuedAt(Date())
                .expiration(Date(System.currentTimeMillis() + 60_000))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact()

        val headers = headerParser.parseHeaders(idToken)
        val resolvedPublicKey = publicKeyFactory.generatePublicKey(headers, OidcPublicKeyList(listOf(jwk)))
        val claims = tokenParser.parseClaims(idToken, resolvedPublicKey)

        assertEquals("provider-user-1", claims.subject)
        assertEquals("a@b.com", claims["email"])
    }

    private fun BigInteger.toBase64Url(): String {
        var bytes = toByteArray()
        if (bytes[0] == 0.toByte() && bytes.size > 1) bytes = bytes.copyOfRange(1, bytes.size)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
