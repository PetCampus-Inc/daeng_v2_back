> 생성: 2026-09-02 18:05 · 최종 수정: 2026-09-02 18:40

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
  - `id`, `url`, `http`, `api`, `lat`/`lng` 등 업계 전반에서 축약 자체가 표준 용어로 굳어진 것은 대상이 아니다. `lat`/`lng`는 auth 도메인([`UserAddress`](../../src/main/kotlin/com/petcampus/knockdog/domain/auth/domain/UserAddress.kt) 등)에서 이미 도메인부터 DB 컬럼까지 전부 이 이름으로 출시돼 있다.
  - 외부 시스템(크롤링 JSON 등)이 이미 정해준 필드명을 그대로 받는 DTO는 그 경계에서만 원본 이름을 받되, 내부적으로는 명확한 이름으로 바꾸고 `@JsonProperty` 등으로 명시 매핑한다 — 예: [`CrawledMenu`](../../src/main/kotlin/com/petcampus/knockdog/domain/kindergarten/adapter/outbound/seed/CrawledKindergarten.kt)는 원본 JSON 키 `unit_str`을 `@JsonProperty("unit_str")`로 받아 `unitLabel` 필드에 매핑한다.
- **강제 수단**: 없음(약어 여부는 기계적으로 판정하기 어렵다) — 코드 리뷰에서 확인한다.
- **첫 적용 사례**: `KindergartenMenu.unitLabel`/`totalDurationLabel`, `CrawledKindergarten.phoneNumber`(KD3-413).

## 3. DTO와 도메인의 필드명 불일치를 지양한다

같은 개념을 가리키는 필드명이 계층(도메인 → JPA → 시딩 DTO → 응답 DTO)마다 다르면, 계층을 넘나들 때마다 변환 코드가 생기고 어느 이름이 진짜인지 헷갈린다. 외부 경계(§2의 예외)가 아니라면 도메인부터 DTO까지 같은 이름을 쓴다.

- **예**: `Kindergarten.lat`/`lng`는 JPA 컬럼·시딩 DTO(`CrawledKindergarten`)·응답 DTO(`KindergartenSummaryResponse` 등) 전부 `lat`/`lng`로 통일했다. 처음엔 도메인/JPA/시딩 DTO를 `latitude`/`longitude`로 풀어 쓰고 API 응답에서만 `lat`/`lng`로 축약했으나, 계층마다 이름이 달라지고 변환 코드만 늘어난다는 지적으로 전부 `lat`/`lng`로 되돌렸다(KD3-413, 2026-09-02).
- **예외**: 외부 시스템이 이미 정해준 필드명을 그대로 받는 DTO의 경계 지점은 §2의 예외를 따른다 — 그 경계에서만 원본 이름을 받고, 나머지 계층은 서로 통일한다.
- **강제 수단**: 없음 — 코드 리뷰에서 확인한다.

## 4. 참고

- 설계 근거·결정 경위: [`docs/work/KD3-413-kindergarten-static-lookup.md`](../work/KD3-413-kindergarten-static-lookup.md)
