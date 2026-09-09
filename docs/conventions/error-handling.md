> 생성: 2026-08-31 01:05 · 최종 수정: 2026-09-09

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

첫 도입 사례는 auth 도메인의 `domain/auth/application/AuthErrorCode.kt`(KD3-258)다. 구조화 포맷(아래 참고)의 첫 실제 구현 사례는 pet 도메인의 `domain/pet/application/PetErrorCode.kt`(KD3-431)다.

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

```kotlin
enum class PetErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "PET-404-1", "해당 강아지가 존재하지 않습니다."),
    NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "PET-403-1", "해당 강아지에 접근할 권한이 없습니다."),
    LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "PET-400-1", "강아지는 최대 5마리까지 등록할 수 있어요."),
    RELATIONSHIP_TEXT_REQUIRED(HttpStatus.BAD_REQUEST, "PET-400-2", "관계를 직접 입력해 주세요."),
    NOT_FOUND_BREED(HttpStatus.BAD_REQUEST, "PET-400-3", "존재하지 않는 견종입니다."),
}
```

`enum` 상수 이름 자체(`NOT_FOUND` 등)는 `AuthErrorCode`처럼 도메인 접두어 없이 짧게 쓴다 — 프론트와의 계약은 `code` 문자열 값(`"PET-404-1"` 등)이지 Kotlin 상수 이름이 아니다.

던질 때는 `throw BusinessException(AuthErrorCode.NOT_FOUND_USER)`처럼 쓰고, 맥락을 덧붙일 게 있으면 두 번째 인자로 메시지를 넘긴다. 위치는 `application` 패키지 바로 아래다 — 에러 코드는 유스케이스가 결정하는 것이지 순수 도메인 모델이나 어댑터의 관심사가 아니다.

### code 문자열 값은 프론트와의 계약이다

레거시 자바 서버를 확인한 결과, `code` 문자열은 프론트가 `switch`/`Set` 등으로 직접 비교해 분기하는 데 쓰인다(예: `interceptor`의 토큰 갱신 분기, 로그인 실패 시 탈퇴/재가입제한 분기, 원장 인증 에러 처리). 포맷은 도메인마다 다르다 — auth는 시맨틱 문자열(`EXPIRED_TOKEN`, `WITHDRAWN_USER`), 그 외는 `<도메인>-<HTTP status>-<순번>` 구조화 문자열(`PET-404-1`, `OWNER_VERIFICATION-401-1`). 어느 쪽이든 **프론트 상수와 값이 정확히 일치해야** 프론트 분기 로직이 깨지지 않는다.

새 서버로 도메인을 마이그레이션할 때(v0→v1 전환 기간 포함) `ErrorCode.code` 값은 프론트가 이미 참조 중인 문자열을 그대로 가져다 쓴다. 포맷을 새로 통일하고 싶다면, 반드시 프론트 코드(`daeng_v2_front`)를 함께 수정하는 작업으로 스코프를 잡아야 한다 — 백엔드만 바꾸면 안 된다.

**프론트가 실제로 분기하지 않는 경우에도 레거시 값을 재사용한다.** `PetErrorCode`(KD3-431) 도입 시 `daeng_v2_front`를 확인한 결과 pet 관련 에러는 `code` 값으로 분기하지 않고 전부 일시적 오류 토스트로 처리하고 있었다 — 그래도 레거시(`PET-404-1` 등)를 그대로 재사용했다. "프론트가 안 쓰면 자유롭게 바꿔도 된다"가 아니라 "재사용이 기본값이고, 바꾸려면 프론트도 같이 고치는 별도 스코프"라는 원칙은 분기 여부와 무관하게 적용한다.

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
6. `MethodArgumentTypeMismatchException`(경로 변수·쿼리 파라미터 타입 불일치, 예: `Long` 자리에 숫자 아닌 값) → 400 + `CommonErrorCode.INVALID_INPUT_VALUE` (메시지는 파라미터 타입 등 내부 정보 노출 방지를 위해 고정 문구). 이 핸들러가 없으면 Spring이 원래 자동으로 400 처리해주는 것을 catch-all이 가로채 500으로 만들어버린다 — 새 기능이 아니라 프레임워크 기본 동작을 되살리는 핸들러다(KD3-432)
7. `MissingRequestCookieException`(필수 쿠키 누락) → 400 + `CommonErrorCode.INVALID_INPUT_VALUE`. 인증 토큰을 쿠키로 받는 API(`/api/v1/auth/login`, `/refresh`, `POST /api/v1/users`)를 쿠키 없이 호출하면 발생한다 — 클라이언트 실수이므로 500이 아니라 400으로 내린다
8. `OptimisticLockingFailureException`(`@Version` 낙관적 락 충돌 — 저장 시점에 다른 트랜잭션이 먼저 같은 행을 바꿔서 버전이 안 맞는 경우) → 409 + `CommonErrorCode.RESOURCE_CONFLICT`(KD3-431, `PetJpaEntity.version` 도입과 함께 추가)
9. `HttpRequestMethodNotSupportedException`(Spring이 던지는 405) → 405 + `CommonErrorCode.METHOD_NOT_ALLOWED`
10. 그 외 `Exception` → 500 + `CommonErrorCode.INTERNAL_SERVER_ERROR`

2~3번은 하위 호환을 위해 남겨둔 것이다 — `BusinessException`을 쓰지 않는 기존 코드(예: `GetOwnerService`)가 아직 있다. **새로 작성하는 코드는 2~3번 대신 `BusinessException` + 도메인별 `ErrorCode`를 쓴다.**

catch-all(10번)이 프레임워크가 던지는, 아직 전용 핸들러가 없는 예외까지 500으로 마스킹할 수 있다는 점은 여전히 알려진 한계다 — 4~9번은 실제로 겪은 케이스를 좁혀서 처리한 것이고, `@Valid` 기반 필드별 검증 실패 응답 포맷은 아직 다루지 않았다(티켓 KD3-257의 7번 항목에서 별도로 정리 예정). **새로운 전용 핸들러를 추가할 땐 catch-all을 없애거나 고치는 게 아니라, 이 목록에 구체적인 예외 타입 핸들러를 하나 더 추가하는 방식을 따른다** — catch-all은 예상 못한 예외의 최후 안전망으로 남겨두고, 알려진 예외는 이렇게 하나씩 구체적으로 잡아나가는 것이 정석이다.

`GlobalExceptionHandler.kt`에는 이 처리 우선순위를 설명하는 주석을 코드에 남기지 않는다(`code-style.md` §1, 주석 금지) — 각 핸들러의 근거는 이 문서에 둔다.

`IllegalStateException`(`check()` 실패)에 대한 전역 핸들러는 두지 않는다. 도메인 메서드가 던지는 상태 위반이 실제로 발생할 수 있는 호출부라면, 그 서비스가 직접 잡아 해당 API에 맞는 `BusinessException`/에러코드로 변환한다 — `IllegalStateException` 타입 자체를 전역으로 잡으면 무관한 다른 도메인의 예상 못한 버그까지 오분류될 위험이 있어 채택하지 않았다.

## 4. 참고

- 설계 근거·트레이드오프: [`docs/work/KD3-257-common-response-error-handling.md`](../work/KD3-257-common-response-error-handling.md)
- 코드: `src/main/kotlin/com/petcampus/knockdog/global/exception/`
