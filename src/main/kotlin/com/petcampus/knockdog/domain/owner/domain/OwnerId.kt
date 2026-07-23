package com.petcampus.knockdog.domain.owner.domain

import java.util.UUID

@JvmInline
value class OwnerId(
    val value: String,
) {
    companion object {
        fun generate(): OwnerId = OwnerId(UUID.randomUUID().toString())
    }
}
