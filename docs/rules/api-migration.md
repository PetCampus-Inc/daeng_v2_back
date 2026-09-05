> 생성: 2026-08-02 13:45 · 최종 수정: 2026-09-03 11:35

# API 이관 규칙

이 문서는 레거시 API를 신규 서버로 이관할 때 지킬 계약 보존, 버전, 판정 원칙을 정의한다. 실제 API별 판정은 [`docs/inventory/api.md`](../inventory/api.md)에서 관리한다.

**적용 범위는 레거시 이관이다.** 신규 서버가 실제로 어떤 응답 형태를 쓰는지(`Response<T>` 필드, 에러 코드 체계)는 [`docs/conventions/api-contract.md`](../conventions/api-contract.md)와 [`docs/conventions/error-handling.md`](../conventions/error-handling.md)에 둔다. 이 문서의 규칙은 `v0` 컷오버가 끝나면 폐기 대상이다.

## 1. 기본 원칙

- **신규 서버는 `v0` 엔드포인트를 만들지 않는다**([`ADR 0012`](../adr/0012-신규-서버-v0-미제공-원칙.md)). 이관 대상 API는 계약 변경 여부와 무관하게 전부 `v1`(또는 그에 준하는 신규 버전) 경로로 구현한다.
- 기존 `v0`를 호출하는 클라이언트(구버전 앱, 아직 `v1`로 전환하지 않은 프론트)는 레거시 서버가 계속 담당한다 — 라우팅/LB 설정의 문제이지 신규 서버가 구현할 대상이 아니다.
- 계약(응답 내용) 자체는 `KEEP` 판정이면 레거시와 동일하게 맞춘다 — **단, 문서에 근거를 남긴 레거시 버그 수정은 예외다**(§3). 경로가 바뀌는 것과 응답 내용이 바뀌는 것은 별개 결정이다 — 경로는 항상 바뀌지만, 내용까지 재설계할지는 API별로 판단한다.
- 미사용, 중복, 위험 API는 신규 서버로 이관하지 않는다.
- AI가 추정한 사용 여부는 확정 근거가 아니며, 코드 검색·로그·사람 확인 중 하나 이상의 근거로 보강한다.
- **소급 적용하지 않는다.** 이 원칙 이전에 이미 구현·머지된 `v0` 엔드포인트(auth 도메인의 `login`/`refresh`/`logout`/약관 동의 제출·조회 등)는 자동으로 제거 대상이 아니다. 제거 여부는 그 도메인에서 별도로 판단한다.

## 2. 새 경로 이름

`v1`(또는 그에 준하는 신규 버전) 경로 이름은 레거시 이름을 그대로 베끼지 않는다. 아래 중 하나라도 해당하면 특히 신경 써서 다시 짓는다 — RESTful하지 않은 레거시 이름을 그대로 옮기면 `v1`을 만든 의미가 없다.

| 유형 | 예 |
|---|---|
| 경로에 동사가 있다 | `GET /mypage/getUserInfo` → `GET /v1/users/me` |
| HTTP 메서드를 body 필드로 대체했다 | `POST /mypage/address`의 `operation: ADD\|UPDATE\|DELETE` → `POST`/`PATCH`/`DELETE` |
| 메서드가 의미와 다르다 | `POST /pet/remove/{id}` → `DELETE /v1/pets/{id}` |
| 이름이 실제 동작과 다르다 | `GET /kindergarten/filters/result`(개수 반환) → `GET /v1/kindergartens/count` |
| 경로가 실제 호출 주체를 오도한다 | `/api/v0/admin/owner-verification/**`(실제로는 원장 본인) → `/api/v1/owner-verifications/**` |

이름을 새로 지었다면 **무엇이 달라서 이렇게 지었는지를 해당 도메인 문서(`docs/domains/<domain>.md`)에 남긴다.** 근거가 없으면 다음 사람이 "이건 왜 이 이름이지"에서 막힌다.

### 판정 기록

`v0` 경로 자체는 신규 서버에 없으므로, [`docs/inventory/api.md`](../inventory/api.md)의 `대상 버전` 열은 신규 서버가 실제로 제공하는 버전(`v1` 등)만 남긴다. `v0`는 레거시 서버가 계속 서비스한다는 의미로 `-`(해당 없음)로 둔다.

## 3. 계약 보존 대상

`KEEP` API는 최소한 아래 항목을 확인한다. `Path`는 항상 새 경로로 바뀌므로 비교 대상이 아니다 — 나머지 항목이 레거시와 기능적으로 동일한지를 본다.

| 항목 | 기준 |
|---|---|
| Method | 기존 HTTP method 유지(REST 관례에 맞게 조정하는 경우는 §2 참고) |
| Request field | 필드명, 필수 여부, 타입, 기본값 유지 |
| Response field | 필드명, 타입, null/빈 배열 처리 유지 |
| Status code | 기존 성공/실패 status code 유지 |
| Error code / message | 프론트가 분기하는 값은 변경 금지 |
| Date/time format | 기존 포맷과 timezone 처리 유지 |
| 정렬/페이징 | 기본 정렬, cursor/page 의미 유지 |

레거시가 버그로 잘못된 값을 내려주고 있었다면(예: `roadAddress` 필드에 실제로는 다른 주소 값이 들어가는 경우), 그대로 이식할지 고칠지를 판단해 도메인 문서에 근거를 남긴다 — 경로가 이미 바뀌는 이상 버그까지 그대로 옮길 필요는 없지만, 프론트가 그 버그에 의존하고 있을 수 있으니 확인 없이 고치지 않는다.

## 4. Breaking change 기준

아래 변경은 breaking change로 본다. path 변경 자체는 포함하지 않는다 — §1에 따라 항상 바뀌기 때문이다.

- method 변경
- request/response 필드 삭제, 이름 변경, 타입 변경
- optional 필드를 required로 변경
- enum 값 삭제 또는 의미 변경
- status code, error code, 프론트 분기용 message 변경
- 날짜 포맷, null 처리, 정렬 순서 변경

## 5. Parity 기준

- `KEEP` API는 레거시와 신규 응답을 비교해 검증한다. **자동 golden 테스트는 없다** — 레거시가 다른 저장소라 CI에서 띄울 수 없다. 절차는 [`003-migration.md`](../workflows/003-migration.md) 4단계 "`KEEP` API 로컬 응답 대조"를 따르고, 대조 결과와 제한을 작업 문서에 남긴다.
- `REDESIGN`, `DROP` API는 대조 제외 근거를 남기고, `DEFER` API는 대상 여부를 후속 확인으로 둔다.
- 대조에서 차이를 발견하면 프론트 저장소에서 그 필드를 실제로 읽는지 확인한 뒤 허용 여부를 판단한다. 확인 없이 "안 쓸 것 같다"로 넘기지 않는다.
