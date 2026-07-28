> 생성: 2026-07-28 23:34 · 최종 수정: 2026-07-28 23:55

# KD3-257 공통 응답/에러 처리 계층 구축

티켓 본문의 착수 항목 10개 중 **블로킹 항목(1~4)만** 이번 계획의 범위다. 5~10은 별도로 다룬다([작업 제외 범위](#작업-제외-범위) 참고).

## 작업 목표

도메인별 개발을 시작하기 전에, 모든 도메인이 공통으로 의존할 응답 포맷(`Response<T>`)과 에러 처리 계층(`ErrorCode`, `BusinessException`, `GlobalExceptionHandler`)을 구축한다. 완료되면 이후 도메인(auth 등) 작업에서 컨트롤러가 바로 이 공통 계층 위에서 응답/예외를 다룰 수 있다.

## 작업 범위

```
global/exception/
  ErrorCode.kt            신규 — 인터페이스: code, status(HttpStatus), message
  CommonErrorCode.kt      신규 — enum, ErrorCode 구현. 도메인 무관 공통 에러만
                           (INVALID_INPUT_VALUE 400, RESOURCE_NOT_FOUND 404,
                            METHOD_NOT_ALLOWED 405, INTERNAL_SERVER_ERROR 500 등)
  BusinessException.kt    신규 — open class, ErrorCode를 받는 RuntimeException 베이스
  GlobalExceptionHandler.kt  수정 — BusinessException 핸들러 추가, 응답 바디를 Response<T>로 교체
                              (기존 IllegalArgumentException/NoSuchElementException 핸들러는 유지)

global/response/
  Response.kt             신규 — 공통 응답 래퍼 (아래 방향 논의 참고)
```

## 작업 제외 범위

- **티켓의 5~10번 항목** (인증 컨텍스트 접근 방식, `SecurityFilterChain` 뼈대, Validation 실패 응답 포맷, 페이지네이션 공통 DTO, ID 생성 전략, 로깅/요청 추적) — 범위가 넓어 이번 세션은 도메인 개수와 무관하게 선행이 필요한 1~4번만 우선 처리하기로 결정. 5~6은 auth 착수 직전, 7~10은 auth 작업과 병행 가능하므로 각각 별도 작업 단위로 진행.
- **도메인별 `ErrorCode` enum**(`AuthErrorCode` 등) — 아직 어떤 도메인 마이그레이션도 시작하지 않았다. 각 도메인 착수 시 그 도메인 패키지 안에서 `ErrorCode`를 구현하는 enum을 추가한다.
- **`GetOwnerService`의 기존 `NoSuchElementException` 사용을 `BusinessException`으로 교체하는 리팩터** — owner 도메인 슬라이스 작업 범위. 이번 티켓은 `GlobalExceptionHandler`가 두 방식(레거시 스타일 예외 + 신규 `BusinessException`)을 모두 처리하도록만 한다.
- **기존 컨트롤러(`OwnerController` 등)의 성공 응답을 `Response<T>`로 감싸는 작업** — 이번 티켓은 공통 계층(`Response<T>`/`ErrorCode`/`BusinessException`/`GlobalExceptionHandler`)을 만드는 것까지가 범위다. 티켓 본문 1~4번 항목 어디에도 기존 컨트롤러 마이그레이션은 없다. `OwnerController.register`/`getOne`은 여전히 `OwnerResponse`/`ResponseEntity<OwnerResponse>`를 raw로 반환하며, 이는 위 `GetOwnerService` 예외 교체와 같은 이유로 owner 도메인 슬라이스 작업에서 다룬다. 따라서 현재 `Response<T>`는 에러 경로(`GlobalExceptionHandler`)에서만 쓰이고 성공 경로에는 아직 어디서도 쓰이지 않는다 — 의도된 상태다.

## 방향 논의 및 결정 사항

**`Response<T>` 필드 구성**: 레거시 자바 서버의 `Response.java`(`status`/`code`/`message`/`data`/`responseTime`)를 그대로 가져올지, 아니면 `success`/`error` 형태로 재설계할지 검토했다. 프론트엔드 개발자 확인 결과 프론트가 기존 파싱 로직 재사용을 원했고, 실제로 프론트 저장소(`daeng_v2_front`)를 확인해보니:
- `shared/api/model/response.ts`의 `ApiResponse<T>` 타입이 `status`/`code`/`message`/`data` 4개 필드로 정의되어 있고
- `status`는 `getUploadImage.ts`, `apps/mobile/bridges/api/image.ts`에서 `response.status === 200`으로 실제 분기에 쓰이며 (HTTP 레벨 status와 별개로 **바디 필드**를 직접 비교)
- `code`는 `interceptor/index.ts`의 토큰 갱신 분기(`TOKEN_ERROR_CODE.EXPIRED_TOKEN` 등), `useLogin.ts`의 탈퇴/재가입제한 분기(`LOGIN_ERROR_CODE.WITHDRAWN_USER`, `REJOINING_RESTRICTION_PERIOD`), `ownerVerificationError.ts`의 세분화된 에러 처리(`OWNER_VERIFICATION-409-2` 등)에 실제로 쓰인다
- `responseTime`은 프론트 어디에서도 파싱하지 않는다

→ **레거시와 동일한 4개 필드(`status`/`code`/`message`/`data`)를 그대로 유지하고 `responseTime`만 제외**하기로 결정. `code`는 nullable(성공 시 생략 가능), 나머지는 non-null.

**`ErrorCode` 설계**: 레거시는 전 도메인 에러코드를 하나의 flat enum(`common.response.ErrorCode`)에 몰아넣고 auth만 예외적으로 별도 `AuthErrorCode`를 썼다 — 이 비일관성이 이번 티켓이 정리하려는 문제다. 새 서버는 `ErrorCode`를 **인터페이스**로 두고, 도메인별 에러코드 enum이 각자 그 도메인 패키지 안에서 구현하도록 한다. 이번 범위(1~4)에서는 도메인별 enum 없이 `CommonErrorCode`(도메인 무관 공통 에러)만 추가한다.

**코드 문자열 값의 프론트 계약 (중요, 후속 작업 시 반드시 지킬 것)**: 레거시를 뜯어본 결과 `code` 문자열 포맷이 두 가지가 혼재한다 — auth는 시맨틱 문자열(`EXPIRED_TOKEN`, `WITHDRAWN_USER`), 그 외(owner-verification 등)는 구조화 문자열(`OWNER_VERIFICATION-401-1`). 두 포맷 모두 프론트 상수(`LOGIN_ERROR_CODE`, `TOKEN_ERROR_CODE`, `OWNER_VERIFICATION_MESSAGE_KEY`)와 값이 정확히 일치해야 프론트 분기 로직이 깨지지 않는다. **이번 티켓은 포맷을 통일하지 않는다** — 포맷 통일 여부는 각 도메인 마이그레이션 시 판단하되, v0/v1을 프론트가 동시에 호출하는 전환 기간에는 기존 코드 문자열 값을 그대로 유지해야 한다.

**`GlobalExceptionHandler` 하위 호환**: `GetOwnerService`가 이미 `NoSuchElementException`을 던지고 있어(`domain/owner/application/service/GetOwnerService.kt:17`), 기존 핸들러를 제거하지 않고 응답 바디만 새 `Response<T>` 포맷으로 교체한다. `BusinessException` 핸들러가 우선순위를 갖고, 매핑 안 된 나머지 `Exception`은 `CommonErrorCode.INTERNAL_SERVER_ERROR`(500)로 처리한다.

## 완료 확인 기준

- `./gradlew ktlintCheck` 통과
- `./gradlew build` 성공 (기존 `GetOwnerService` 관련 테스트 포함, 회귀 없음)
- 신규 단위 테스트:
  - `BusinessException` → `GlobalExceptionHandler`가 해당 `ErrorCode`의 status/code/message로 `Response<T>`를 만드는지
  - 기존 `IllegalArgumentException`/`NoSuchElementException` 발생 시에도 새 `Response<T>` 포맷(바디에 `status`/`code`/`message`/`data` 필드)으로 응답하는지
  - 매핑 안 된 일반 `Exception` 발생 시 500 + `CommonErrorCode.INTERNAL_SERVER_ERROR`로 응답하는지
  - Spring이 던지는 `HttpRequestMethodNotSupportedException`(허용 안 된 HTTP 메소드)이 500이 아니라 405 + `CommonErrorCode.METHOD_NOT_ALLOWED`로 응답하는지 — catch-all `Exception` 핸들러가 프레임워크 예외까지 삼켜 상태 코드를 마스킹하지 않는지 확인하는 회귀 테스트
- `Response<T>`의 필드 구성(`status`/`code`/`message`/`data`)이 프론트 `ApiResponse<T>` 타입과 일치하는지는 `ResponseTest.kt`의 단위 테스트로 검증한다. 실제 컨트롤러를 통한 수동 확인(HTTP 호출)은 기존 컨트롤러가 아직 `Response<T>`를 쓰지 않으므로 이번 범위에서 수행하지 않는다 — 도메인 슬라이스 작업이 `Response<T>`를 채택할 때 그 작업의 완료 기준에 포함한다.

## 작업 후 확인 목록

- `docs/architecture/common-response-error.md` 작성 완료 — `Response<T>` 필드 구성, `ErrorCode` 확장 방식(도메인별 enum이 인터페이스 구현), "코드 문자열 값의 프론트 계약" 제약을 포함.
- (리뷰에서 발견, 후속 티켓으로 분리 권장) `GlobalExceptionHandler`의 catch-all `Exception` 핸들러가 `HttpRequestMethodNotSupportedException`(405) 외의 프레임워크 예외(예: 존재하지 않는 라우트)도 전부 500으로 마스킹할 수 있다. Validation 실패 응답 포맷(티켓 7번 항목) 착수 시 함께 정리한다.
