package com.petcampus.knockdog.domain.auth.domain

import java.security.SecureRandom

/** 외부 노출용 공개 코드 (8자 영숫자). 레거시 `user.userId`(ShortIdGenerator) 대응. */
@JvmInline
value class UserCode(
    val value: String,
) {
    init {
        require(value.length == LENGTH) { "UserCode는 ${LENGTH}자여야 합니다: $value" }
    }

    companion object {
        private const val LENGTH = 8
        private val ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()
        private val RANDOM = SecureRandom()

        fun generate(): UserCode {
            val bytes = ByteArray(LENGTH)
            RANDOM.nextBytes(bytes)
            val chars = CharArray(LENGTH) { i -> ALPHABET[(bytes[i].toInt() and 0xFF) % ALPHABET.size] }
            return UserCode(String(chars))
        }
    }
}
