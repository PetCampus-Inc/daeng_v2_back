package com.petcampus.knockdog.global.response

import com.petcampus.knockdog.global.exception.ErrorCode
import org.springframework.http.HttpStatus

data class Response<T>(
    val status: Int,
    val code: String? = null,
    val message: String,
    val data: T? = null,
) {
    companion object {
        private const val DEFAULT_SUCCESS_MESSAGE = "정상 처리되었습니다."

        fun <T> success(
            data: T? = null,
            message: String = DEFAULT_SUCCESS_MESSAGE,
        ): Response<T> = Response(status = HttpStatus.OK.value(), message = message, data = data)

        fun error(
            errorCode: ErrorCode,
            message: String? = null,
        ): Response<Unit> =
            Response(
                status = errorCode.status.value(),
                code = errorCode.code,
                message = message ?: errorCode.message,
            )
    }
}
