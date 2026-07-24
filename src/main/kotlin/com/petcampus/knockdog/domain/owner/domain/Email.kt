package com.petcampus.knockdog.domain.owner.domain

@JvmInline
value class Email(
    val value: String,
) {
    init {
        require(EMAIL_REGEX.matches(value)) { "올바른 이메일 형식이 아닙니다: $value" }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
