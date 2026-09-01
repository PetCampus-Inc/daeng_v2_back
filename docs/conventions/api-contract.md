> 생성: 2026-08-31 01:05 · 최종 수정: 2026-09-01 11:10

# API 응답 계약

모든 도메인 컨트롤러가 따르는 공통 응답 형태를 정의한다. 예외 처리와 에러 코드는 [`error-handling.md`](error-handling.md)에 둔다. 레거시 `v0` 계약을 이관할 때 무엇을 보존해야 하는지는 [`docs/rules/api-migration.md`](../rules/api-migration.md)를 따른다.

왜 이 형태로 정했는지는 [`KD3-257 작업 문서`](../work/KD3-257-common-response-error-handling.md) §방향 논의 및 결정 사항을 참고한다.

## 1. `Response<T>`

`global/response/Response.kt`. 필드는 `status`/`code`/`message`/`data` 4개뿐이다.

```kotlin
data class Response<T>(
    val status: Int,
    val code: String? = null,
    val message: String,
    val data: T? = null,
)
```

- `Response.success(data)` — 200 + `data` + `code: "SUCCESS"`
- `Response.success(data, message, code)` — 성공에도 결과를 구분해야 하는 API는 코드를 직접 넘긴다 (레거시 이메일 인증이 `ALREADY_VERIFIED` 같은 값을 쓰는 방식)
- `Response.error(errorCode, message)` — `errorCode.status` + `errorCode.code` + (커스텀 메시지 없으면 `errorCode.message`)

**성공 응답의 `code`는 `"SUCCESS"`다.** KD3-257에서는 "성공 시 생략 가능"으로 뒀으나, 레거시가 성공에도 `code: "SUCCESS"`를 내리고 **프론트가 그 값으로 분기하는 곳이 있어**(`features/address-picker/api/searchAddress.ts`의 `code !== 'SUCCESS'`) `v0` 계약 유지를 위해 채우는 쪽으로 바꿨다 (KD3-258).

**필드를 임의로 추가/삭제하지 않는다.** 이 4개 필드는 프론트(`daeng_v2_front`) `shared/api/model/response.ts`의 `ApiResponse<T>` 타입과 정확히 매칭되어 있고, 프론트가 실제로 파싱해서 쓰는 값이다(바디의 `status === 200` 성공 판정, `code` 기반 에러 분기 등). 필드를 바꾸면 프론트도 함께 수정해야 한다.

## 2. 참고

- 응답 형식 통일 결정: [`0004`](../adr/0004-api-v0-유지-v1-신규.md)
- 설계 근거·트레이드오프: [`docs/work/KD3-257-common-response-error-handling.md`](../work/KD3-257-common-response-error-handling.md)
- 코드: `src/main/kotlin/com/petcampus/knockdog/global/response/`
