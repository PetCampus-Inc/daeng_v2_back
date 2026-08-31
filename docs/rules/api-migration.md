> 생성: 2026-08-02 13:45 · 최종 수정: 2026-09-01 10:30

# API 이관 규칙

이 문서는 레거시 API를 신규 서버로 이관할 때 지킬 계약 보존, 버전, 판정 원칙을 정의한다. 실제 API별 판정은 [`docs/inventory/api.md`](../inventory/api.md)에서 관리한다.

**적용 범위는 레거시 이관이다.** 신규 서버가 실제로 어떤 응답 형태를 쓰는지(`Response<T>` 필드, 에러 코드 체계)는 [`docs/conventions/api-contract.md`](../conventions/api-contract.md)와 [`docs/conventions/error-handling.md`](../conventions/error-handling.md)에 둔다. 이 문서의 규칙은 `v0` 컷오버가 끝나면 폐기 대상이다.

## 1. 기본 원칙

- 프론트/앱이 실제 사용 중인 `v0` API는 기본적으로 기존 계약을 유지한다.
- 계약을 깨는 변경이 필요하면 기존 `v0`를 바꾸지 않고 `v1` 등 새 경로로 추가한다.
- 미사용, 중복, 위험 API는 신규 서버로 이관하지 않는다.
- AI가 추정한 사용 여부는 확정 근거가 아니며, 코드 검색·로그·사람 확인 중 하나 이상의 근거로 보강한다.

## 2. `v1`을 새로 만들 것인가

**기본값은 `v0` 단독이다.** 재설계할 것이 있을 때만 `v1`을 만든다.

### `v1` 트윈을 만들지 않는다

경로도 계약도 그대로 두면 되는 API는 `v0`로만 이관하고 끝낸다. 같은 API를 `v0`와 `v1` 양쪽에 두지 않는다.

- 유지비가 2배가 된다 — 컨트롤러, 테스트, 명세 문서가 두 벌이 된다.
- 프론트가 옮길 이유가 없다. 경로가 같으면 `v1`으로 이사할 동기가 0이라 **`v0`가 영영 죽지 않는다.**
- `v1`의 목적은 정리인데, 정리할 게 없으면 존재 의미가 없다.

### `v1` 대상 판정

아래 중 하나라도 해당하면 `v1`을 만든다. 어디에도 해당하지 않으면 `v0` 단독이다.

| 유형 | 예 |
|---|---|
| 경로에 동사가 있다 | `GET /mypage/getUserInfo` → `GET /v1/users/me` |
| HTTP 메서드를 body 필드로 대체했다 | `POST /mypage/address`의 `operation: ADD\|UPDATE\|DELETE` → `POST`/`PATCH`/`DELETE` |
| 메서드가 의미와 다르다 | `POST /pet/remove/{id}` → `DELETE /v1/pets/{id}` |
| 이름이 실제 동작과 다르다 | `GET /kindergarten/filters/result`(개수 반환) → `GET /v1/kindergartens/count` |
| 경로가 실제 호출 주체를 오도한다 | `/api/v0/admin/owner-verification/**`(실제로는 원장 본인) → `/api/v1/owner-verifications/**` |

### 경로가 그대로여도 `v1`을 만드는 예외

경로만 안 바뀔 뿐 사실상 재설계인 경우가 있다. 둘 뿐이다.

1. **응답 계약을 깨는 변경이 필요할 때** — 필드 제거, 공통 응답 래퍼 변경 등. `v0`에 넣으면 프론트가 깨진다.
2. **인증·권한 시맨틱이 바뀌어야 하는데 `v0` 클라이언트를 깰 수밖에 없을 때.**

이 예외로 `v1`을 만들었다면 **무엇이 달라서 `v1`인지를 해당 도메인 문서(`docs/domains/<domain>.md`)에 남긴다.** 경로가 같은 `v1`은 나중에 보면 트윈과 구분되지 않는다 — 근거가 없으면 다음 사람이 "이건 왜 v1이지"에서 막힌다.

### 판정 기록

`v1` 신설 여부는 [`docs/inventory/api.md`](../inventory/api.md)의 `대상 버전` 열에 남긴다 — `v0`(단독), `v0+v1`(둘 다), `없음`(미이관).

## 3. 계약 보존 대상

`KEEP` API는 최소한 아래 항목을 확인한다.

| 항목 | 기준 |
|---|---|
| Path / method | 기존 호출 경로와 HTTP method 유지 |
| Request field | 필드명, 필수 여부, 타입, 기본값 유지 |
| Response field | 필드명, 타입, null/빈 배열 처리 유지 |
| Status code | 기존 성공/실패 status code 유지 |
| Error code / message | 프론트가 분기하는 값은 변경 금지 |
| Date/time format | 기존 포맷과 timezone 처리 유지 |
| 정렬/페이징 | 기본 정렬, cursor/page 의미 유지 |

## 4. Breaking change 기준

아래 변경은 breaking change로 본다.

- API path 또는 method 변경
- request/response 필드 삭제, 이름 변경, 타입 변경
- optional 필드를 required로 변경
- enum 값 삭제 또는 의미 변경
- status code, error code, 프론트 분기용 message 변경
- 날짜 포맷, null 처리, 정렬 순서 변경

## 5. Parity 기준

- `KEEP` API는 레거시와 신규 응답을 비교해 검증한다. **자동 golden 테스트는 없다** — 레거시가 다른 저장소라 CI에서 띄울 수 없다. 절차는 [`003-migration.md`](../workflows/003-migration.md) 4단계 "`KEEP` API 로컬 응답 대조"를 따르고, 대조 결과와 제한을 작업 문서에 남긴다.
- `REDESIGN`, `DROP` API는 대조 제외 근거를 남기고, `DEFER` API는 대상 여부를 후속 확인으로 둔다.
- 대조에서 차이를 발견하면 프론트 저장소에서 그 필드를 실제로 읽는지 확인한 뒤 허용 여부를 판단한다. 확인 없이 "안 쓸 것 같다"로 넘기지 않는다.
