> 생성: 2026-08-02 13:45 · 최종 수정: 2026-08-31 01:05

# API 이관 규칙

이 문서는 레거시 API를 신규 서버로 이관할 때 지킬 계약 보존, 버전, 판정 원칙을 정의한다. 실제 API별 판정은 [`docs/inventory/api.md`](../inventory/api.md)에서 관리한다.

**적용 범위는 레거시 이관이다.** 신규 서버가 실제로 어떤 응답 형태를 쓰는지(`Response<T>` 필드, 에러 코드 체계)는 [`docs/conventions/api-contract.md`](../conventions/api-contract.md)와 [`docs/conventions/error-handling.md`](../conventions/error-handling.md)에 둔다. 이 문서의 규칙은 `v0` 컷오버가 끝나면 폐기 대상이다.

## 1. 기본 원칙

- 프론트/앱이 실제 사용 중인 `v0` API는 기본적으로 기존 계약을 유지한다.
- 계약을 깨는 변경이 필요하면 기존 `v0`를 바꾸지 않고 `v1` 등 새 경로로 추가한다.
- 미사용, 중복, 위험 API는 신규 서버로 이관하지 않는다.
- AI가 추정한 사용 여부는 확정 근거가 아니며, 코드 검색·로그·사람 확인 중 하나 이상의 근거로 보강한다.

## 2. 계약 보존 대상

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

## 3. Breaking change 기준

아래 변경은 breaking change로 본다.

- API path 또는 method 변경
- request/response 필드 삭제, 이름 변경, 타입 변경
- optional 필드를 required로 변경
- enum 값 삭제 또는 의미 변경
- status code, error code, 프론트 분기용 message 변경
- 날짜 포맷, null 처리, 정렬 순서 변경

## 4. Parity 기준

- `KEEP` API는 가능한 경우 레거시와 신규 응답 JSON을 비교하는 golden/parity 테스트 대상으로 삼는다.
- `REDESIGN`, `DROP` API는 parity 제외 근거를 남기고, `DEFER` API는 parity 대상 여부를 후속 확인으로 둔다.
