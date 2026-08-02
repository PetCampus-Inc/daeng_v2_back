package com.petcampus.knockdog.global.exception

open class BusinessException(
    val errorCode: ErrorCode,
    message: String? = null,
) : RuntimeException(message ?: errorCode.message)
