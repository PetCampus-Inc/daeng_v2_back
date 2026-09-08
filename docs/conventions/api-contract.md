> 생성: 2026-08-31 01:05 · 최종 수정: 2026-09-08 17:40

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

## 2. 날짜·시간

응답 본문의 날짜·시간 필드는 아래 형태로만 내려간다.

| 개념 | 응답 DTO 타입 | JSON 예시 |
|---|---|---|
| 날짜 + 시각 | `LocalDateTime` | `"2026-09-05T11:38:48"` (초 고정) |
| 날짜만 | `LocalDate` | `"2026-09-05"` |
| 시각만 | `LocalTime` | `"17:59"` (초가 0), `"17:59:30"` (초가 있음) |

- **오프셋(`+09:00`)이나 `Z`를 붙이지 않는다.** 국내 전용 서비스라 모든 값은 KST 벽시계 기준이며, 프론트도 KST로 해석한다. 외국 타임존은 고려하지 않는다.
- **소수점 이하(밀리·나노초)는 버린다.** `now()`류는 나노초를 갖지만 소비처에서 자릿수가 들쭉날쭉하지 않게 통일한다.
- `LocalDateTime`은 초를 항상 표기한다(`...T11:38:00`) — 타임스탬프라 폭이 일정해야 파싱이 편하다. `LocalTime`은 분 단위 설정값(영업시간 등)이라 초가 0이면 생략한다.
- **배열로 직렬화하지 않는다.** 레거시는 `[2026,9,5,11,38,48]` 형태였고 프론트가 이를 직접 파싱했다. 신규 서버는 문자열로만 내려간다.
- 응답 DTO는 `LocalDateTime`/`LocalDate`/`LocalTime`을 그대로 쓴다. 도메인·영속성 계층도 같은 타입을 쓰므로 경계 변환이 없다. `Instant`/`OffsetDateTime`/`ZonedDateTime`/`java.util.Date`는 응답 필드로 쓰지 않는다(오프셋·`Z`가 붙어 규약을 벗어난다).

**강제 수단**:

- Jackson 직렬화 포맷 — `global/config/JacksonDateTimeConfig.kt`(`LocalDateTime`/`LocalDate`/`LocalTime` 포맷 고정, `WRITE_DATES_AS_TIMESTAMPS` 비활성) + `application.yaml`의 `spring.jackson`.
- 서버 시간대 — `LocalDateTime`/`LocalTime`은 시간대 정보가 없어 `now()`가 JVM 기본 시간대에 좌우된다. **배포 환경(컨테이너)에 `TZ=Asia/Seoul`을 주입**해 고정한다([`operations.md`](../inventory/operations.md) — 배포 필수 항목). 테스트는 `build.gradle.kts`가 `user.timezone=Asia/Seoul`로 고정한다(CI 러너는 UTC).
- 회귀 방지 테스트 — `global/config/ResponseDateTimeFormatTest.kt`, `ResponseDateTimeWiringTest.kt`(`@SpringBootTest`).

왜 이렇게 정했는지는 [`KD3-495 작업 문서`](../work/KD3-495-response-datetime-format.md)를 참고한다.

## 3. 참고

- 응답 형식 통일 결정: [`0004`](../adr/0004-api-v0-유지-v1-신규.md)
- 설계 근거·트레이드오프: [`docs/work/KD3-257-common-response-error-handling.md`](../work/KD3-257-common-response-error-handling.md)
- 코드: `src/main/kotlin/com/petcampus/knockdog/global/response/`
