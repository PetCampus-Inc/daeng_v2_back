> 생성: 2026-09-02 18:05 · 최종 수정: 2026-09-02 18:05

# 코드 스타일

주석과 식별자 작명에 대해 모든 도메인이 따르는 규칙을 정의한다. 응답 계약은 [`api-contract.md`](api-contract.md), 예외 처리는 [`error-handling.md`](error-handling.md)에 둔다.

왜 이렇게 정했는지는 [`KD3-413 작업 문서`](../work/KD3-413-kindergarten-static-lookup.md) §방향 논의 및 결정 사항을 참고한다.

## 1. 코드 내 설명 주석을 남기지 않는다

코드 사이에 설명 주석(라인 주석, KDoc 포함)을 작성하지 않는다. 주석 없이도 식별자 이름과 구조만으로 의도가 파악되도록 코드를 쓴다 — 설명이 필요하다고 느껴지면 주석을 추가하는 대신 변수·함수명을 더 명확하게 고치거나 구조를 나눈다.

- **예외**: TODO/FIXME 등 실행 항목을 표시하는 주석은 대상이 아니다.
- **강제 수단**: 없음(ktlint에 주석 자체를 금지하는 규칙은 없다) — 코드 리뷰에서 확인한다.
- **첫 적용 사례**: kindergarten 도메인(`domain/kindergarten/`) 전체에서 기존 주석을 제거했다(KD3-413).

## 2. 식별자(변수·필드·DB 컬럼명)에 약어를 쓰지 않는다

변수명, 필드명, DB 컬럼명에 축약형을 쓰지 않는다. 예: `unitStr`이 아니라 `unitLabel`, `totalDurationStr`이 아니라 `totalDurationLabel`, `tel`이 아니라 `phoneNumber`.

- **예외**:
  - `id`, `url`, `http`, `api` 등 업계 전반에서 축약 자체가 표준 용어로 굳어진 것은 대상이 아니다.
  - **이미 출시된 API 계약의 필드명은 계약을 깨지 않기 위해 유지한다.** 새 API가 기존 계약과 개념을 맞출 때도 그 이름을 따라간다 — 예: kindergarten v1의 좌표 필드(`lat`/`lng`)는 auth 도메인이 이미 출시한 `lat`/`lng` 컨벤션([`UserAddress`](../../src/main/kotlin/com/petcampus/knockdog/domain/auth/domain/UserAddress.kt) 등)과의 일관성을 위해 그대로 두기로 결정했다(2026-09-02).
  - 외부 시스템(크롤링 JSON 등)이 이미 정해준 필드명을 그대로 받는 DTO는 그 경계에서만 원본 이름을 받되, 내부적으로는 명확한 이름으로 바꾸고 `@JsonProperty` 등으로 명시 매핑한다 — 예: [`CrawledMenu`](../../src/main/kotlin/com/petcampus/knockdog/domain/kindergarten/adapter/outbound/seed/CrawledKindergarten.kt)는 원본 JSON 키 `unit_str`을 `@JsonProperty("unit_str")`로 받아 `unitLabel` 필드에 매핑한다.
- **강제 수단**: 없음(약어 여부는 기계적으로 판정하기 어렵다) — 코드 리뷰에서 확인한다.
- **첫 적용 사례**: `KindergartenMenu.unitLabel`/`totalDurationLabel`, `CrawledKindergarten.phoneNumber`/`latitude`/`longitude`(KD3-413).

## 3. 참고

- 설계 근거·결정 경위: [`docs/work/KD3-413-kindergarten-static-lookup.md`](../work/KD3-413-kindergarten-static-lookup.md)
