> 생성: 2026-08-31 01:05 · 최종 수정: 2026-08-31 17:20

# 예외·에러 코드 처리

모든 도메인이 따르는 예외 계층과 에러 코드 규칙을 정의한다. 응답 본문 형태는 [`api-contract.md`](api-contract.md)에 둔다.

왜 이 형태로 정했는지는 [`KD3-257 작업 문서`](../work/KD3-257-common-response-error-handling.md) §방향 논의 및 결정 사항을 참고한다.

## 1. `ErrorCode`

`global/exception/ErrorCode.kt`는 인터페이스다.

```kotlin
interface ErrorCode {
    val code: String
    val status: HttpStatus
    val message: String
}
```

- **도메인 무관 공통 에러**는 `global/exception/CommonErrorCode.kt`(enum)에 둔다.
- **도메인 전용 에러**는 그 도메인 패키지 안에 `<Domain>ErrorCode.kt`(enum)를 만들어 `ErrorCode`를 구현한다. 도메인 착수 시 함께 추가한다.

첫 도입 사례는 auth 도메인의 `domain/auth/application/AuthErrorCode.kt`(KD3-258)다.

```kotlin
enum class AuthErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_TOKEN", "인증 토큰이 만료되었습니다."),
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "NOT_FOUND_USER", "존재하지 않는 회원입니다."),
    // ...
}
```

던질 때는 `throw BusinessException(AuthErrorCode.NOT_FOUND_USER)`처럼 쓰고, 맥락을 덧붙일 게 있으면 두 번째 인자로 메시지를 넘긴다. 위치는 `application` 패키지 바로 아래다 — 에러 코드는 유스케이스가 결정하는 것이지 순수 도메인 모델이나 어댑터의 관심사가 아니다.

### code 문자열 값은 프론트와의 계약이다

레거시 자바 서버를 확인한 결과, `code` 문자열은 프론트가 `switch`/`Set` 등으로 직접 비교해 분기하는 데 쓰인다(예: `interceptor`의 토큰 갱신 분기, 로그인 실패 시 탈퇴/재가입제한 분기, 원장 인증 에러 처리). 포맷은 도메인마다 다르다 — auth는 시맨틱 문자열(`EXPIRED_TOKEN`, `WITHDRAWN_USER`), 그 외는 `<도메인>-<HTTP status>-<순번>` 구조화 문자열(`OWNER_VERIFICATION-401-1`). 어느 쪽이든 **프론트 상수와 값이 정확히 일치해야** 프론트 분기 로직이 깨지지 않는다.

새 서버로 도메인을 마이그레이션할 때(v0→v1 전환 기간 포함) `ErrorCode.code` 값은 프론트가 이미 참조 중인 문자열을 그대로 가져다 쓴다. 포맷을 새로 통일하고 싶다면, 반드시 프론트 코드(`daeng_v2_front`)를 함께 수정하는 작업으로 스코프를 잡아야 한다 — 백엔드만 바꾸면 안 된다.

## 2. `BusinessException`

`global/exception/BusinessException.kt`. 모든 커스텀 예외의 베이스다.

```kotlin
open class BusinessException(val errorCode: ErrorCode, message: String? = null) : RuntimeException(...)
```

도메인 예외는 이걸 상속해서 만든다. 예: `class MemberNotFoundException(id: String) : BusinessException(AuthErrorCode.NOT_FOUND_USER, "회원을 찾을 수 없습니다: $id")`.

## 3. `GlobalExceptionHandler`

`global/exception/GlobalExceptionHandler.kt`. 처리 우선순위:

1. `BusinessException` → `errorCode.status` + `Response.error(errorCode, e.message)`
2. `IllegalArgumentException` → 400 + `CommonErrorCode.INVALID_INPUT_VALUE`
3. `NoSuchElementException` → 404 + `CommonErrorCode.RESOURCE_NOT_FOUND`
4. `HttpMessageNotReadableException`(요청 본문 파싱 실패, 예: 필수 필드 누락) → 400 + `CommonErrorCode.INVALID_INPUT_VALUE` (메시지는 Jackson 내부 정보 노출 방지를 위해 고정 문구)
5. `MissingServletRequestParameterException`(필수 `@RequestParam` 누락) → 400 + `CommonErrorCode.INVALID_INPUT_VALUE`
6. `HttpRequestMethodNotSupportedException`(Spring이 던지는 405) → 405 + `CommonErrorCode.METHOD_NOT_ALLOWED`
7. 그 외 `Exception` → 500 + `CommonErrorCode.INTERNAL_SERVER_ERROR`

2~3번은 하위 호환을 위해 남겨둔 것이다 — `BusinessException`을 쓰지 않는 기존 코드(예: `GetOwnerService`)가 아직 있다. **새로 작성하는 코드는 2~3번 대신 `BusinessException` + 도메인별 `ErrorCode`를 쓴다.**

catch-all(7번)이 프레임워크가 던지는 다른 예외(예: 존재하지 않는 라우트)까지 500으로 마스킹할 수 있다는 점은 여전히 알려진 한계다 — 4~6번은 실제로 겪은 케이스를 좁혀서 처리한 것이고, `@Valid` 기반 필드별 검증 실패 응답 포맷은 아직 다루지 않았다(티켓 KD3-257의 7번 항목에서 별도로 정리 예정).

## 4. 참고

- 설계 근거·트레이드오프: [`docs/work/KD3-257-common-response-error-handling.md`](../work/KD3-257-common-response-error-handling.md)
- 코드: `src/main/kotlin/com/petcampus/knockdog/global/exception/`
