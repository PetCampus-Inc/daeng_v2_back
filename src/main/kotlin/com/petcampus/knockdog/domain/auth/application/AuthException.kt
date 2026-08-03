package com.petcampus.knockdog.domain.auth.application

import com.petcampus.knockdog.global.exception.BaseException
import org.springframework.http.HttpStatus

class AuthException(
    status: HttpStatus,
    message: String,
) : BaseException(status, message) {
    companion object {
        fun invalidProvider(provider: String) = AuthException(HttpStatus.BAD_REQUEST, "지원하지 않는 provider입니다: $provider")

        fun tokenVerificationFailed() = AuthException(HttpStatus.UNAUTHORIZED, "토큰 검증에 실패했습니다.")

        fun invalidToken() = AuthException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.")

        fun expiredToken() = AuthException(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다.")

        fun externalServerError() = AuthException(HttpStatus.BAD_GATEWAY, "소셜 로그인 서버 응답에 실패했습니다.")

        fun notFoundSocialUser() = AuthException(HttpStatus.NOT_FOUND, "소셜 계정을 찾을 수 없습니다.")

        fun notFoundUser() = AuthException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.")

        fun userNotLinked() = AuthException(HttpStatus.CONFLICT, "회원과 연결되지 않은 소셜 계정입니다.")

        fun alreadyLinkedUser() = AuthException(HttpStatus.CONFLICT, "이미 연동된 소셜 계정입니다.")

        fun pendingSocialUser() = AuthException(HttpStatus.CONFLICT, "다른 소셜 계정으로 이미 가입된 이메일입니다. 재연동이 필요합니다.")

        fun withdrawnUser() = AuthException(HttpStatus.FORBIDDEN, "탈퇴한 회원입니다.")

        fun rejoiningRestrictionPeriod() = AuthException(HttpStatus.FORBIDDEN, "탈퇴 후 7일 이내에는 재가입할 수 없습니다.")
    }
}
