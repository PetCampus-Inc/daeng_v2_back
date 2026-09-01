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

        /**
         * 레거시가 성공 응답에 `code: "SUCCESS"`를 내려주고 프론트가 그 값으로 분기하는 곳이 있다
         * (예: `features/address-picker/api/searchAddress.ts`의 `code !== 'SUCCESS'`).
         * v0 계약을 유지하려면 같은 값을 내려야 한다.
         */
        const val SUCCESS_CODE = "SUCCESS"

        /**
         * @param code 성공에도 의미를 구분해야 하는 API는 결과 코드를 직접 넘긴다. 레거시
         *   `Response.success(data, code, message)`가 이메일 인증 결과 등에 쓰는 방식이다.
         */
        fun <T> success(
            data: T? = null,
            message: String = DEFAULT_SUCCESS_MESSAGE,
            code: String = SUCCESS_CODE,
        ): Response<T> = Response(status = HttpStatus.OK.value(), code = code, message = message, data = data)

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
