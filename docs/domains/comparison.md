> 생성: 2026-09-11 · 최종 수정: 2026-09-11

# comparison 도메인 마이그레이션 지시서

보호자가 유치원 2곳을 비교한 **이력**(비교 히스토리)을 담당한다. 비교 결과 계산 자체(`GET /api/v1/kindergartens/comparisons`)는 공개 카탈로그 read라 `kindergarten` 도메인이 담당한다([`kindergarten.md`](kindergarten.md) §1-1) — 이 도메인은 유저 소유 프라이빗 기록만 다룬다.

- 설계 근거: [`0003`](../adr/0003-헥사고날-정석형-통일.md), [`0007`](../adr/0007-인가-기본-deny-전환.md), [`0010`](../adr/0010-신규-db-인스턴스-스키마-재작성.md), [`0012`](../adr/0012-신규-서버-v0-미제공-원칙.md)
- 원본: `daeng_v1_back`(`knockdog_server`)의 `comparison/` 패키지(`ComparisonController` 3개 메서드, `ComparisonHistoryService`, `ComparisonHistory` 엔티티)
- 착수 기록: [`docs/work/KD3-496-comparison-history.md`](../work/KD3-496-comparison-history.md)
- 슬라이스 분리 이유: memo와 동일 — 유저 소유·`user_code` 키·자체 테이블·수명주기·소유자 인가. kindergarten(공개 카탈로그)과 소유 주체·쓰기 패턴·인가 모델이 다름

## 0. 담당 데이터

| 저장소 | 이름 | 비고 |
|---|---|---|
| MySQL (JPA) | `comparison_histories` (V13) | `(user_code, kindergarten_id_a, kindergarten_id_b)` 유니크 1행. `user_code`는 auth 토큰 subject(`UserCode`), `kindergarten_id_*`는 `kindergartens.naver_place_id`. **두 ID를 사전순 정렬해 저장** — `[A,B]`/`[B,A]`를 같은 이력으로 dedup(레거시 `HashSet` 동등성). `BaseEntity` 상속(`created_at`/`updated_at`/`deleted_at`). `compared_at` 컬럼 없이 `updated_at`을 comparedAt으로. FK 제약 없음 |

레거시 `comparison_history`(`userId` String + `kindergartenIds` CSV)를 재설계했다. 레거시는 임의 개수 ID를 담을 수 있는 CSV였으나 실제 비교는 항상 2곳이라 두 컬럼으로 고정했다.

## 1. 불변식 · 제약

| 항목 | 규칙 |
|---|---|
| 비교 대상 수 | 정확히 2곳. `ComparisonHistory.create`가 두 ID가 같으면 `require` 실패 |
| dedup | 정렬 저장 + `UNIQUE(user_code, id_a, id_b)`. 재비교는 네이티브 `INSERT ... ON DUPLICATE KEY UPDATE updated_at = NOW(6), deleted_at = NULL` — `updated_at` 갱신, soft delete됐던 이력은 되살아남 |
| 저장 트리거 | `GET /api/v1/kindergartens/comparisons`(kindergarten) 성공 후, 로그인 상태면 컨트롤러가 `KindergartensComparedEvent`를 발행하고 comparison의 `@EventListener`가 저장한다. kindergarten → comparison 컴파일 의존을 만들지 않으려는 구조(kindergarten이 이벤트 타입만 정의, 리스너는 comparison) |
| 보존 | **개수 상한·프루닝 없음**(레거시대로). 조회만 `limit`으로 제한 |
| 조회 개수 | `?limit=` optional, 기본 10, 1~50으로 clamp(`limit <= 0` → 10, `> 50` → 50) |
| 없어진 유치원 | 응답 `kindergartens` 배열에서 요약 없는 ID는 빠진다(0~2개). 히스토리 행 자체는 남는다. 프론트 `ComparisonHistoryCard`는 2개 미만이면 카드를 숨긴다 |
| 삭제 | `deleted_at` soft delete. 소유자 아니면 `COMPARISON_HISTORY_NOT_OWNER`(403), 없거나 이미 삭제면 `COMPARISON_HISTORY_NOT_FOUND`(404) |
| 인가 | `GET`/`DELETE /api/v1/kindergartens/comparisons/history/**` 인증 필수(`PUBLIC_ENDPOINTS` 미포함). 레거시 `@PrivateAccess`가 permit 규칙에 가려졌던 0004 보안버그 교정 |
| 유치원 요약 | `LoadComparisonKindergartenSummariesPort` → kindergarten `LoadKindergartenPort.findByNaverPlaceIds` 경유. 존재하는 유치원만 반환 |

## 2. 이 서버가 제공하는 API (전부 `v1` 신규 — ADR 0012)

| 신규 (`v1`) | 레거시 (`v0`) | 설명 |
|---|---|---|
| `GET /api/v1/kindergartens/comparisons/history?limit=10` | `GET /api/v0/kindergarten/comparisons/history?limit=10` | 비교 이력 최근순. `[{ id, kindergartens: [{id,name,thumbnailS3Key,categories}], comparedAt }]` |
| `DELETE /api/v1/kindergartens/comparisons/history/{historyId}` | `DELETE /api/v0/kindergarten/comparisons/history/{historyId}` | 이력 soft delete |

## 3. 레거시 대비 계약 변경 (로컬 응답 대조 시 참고)

| 항목 | 레거시 | v1 |
|---|---|---|
| `comparedAt` | `number[]`(`[2026,9,11,10,0,0]`) | `"2026-09-11T10:00:00"` ISO 문자열([`api-contract.md`](../conventions/api-contract.md) §2, KD3-495). `ComparisonHistoryCard`는 렌더에 쓰지 않음 |
| 에러 코드 | `COMPARISON-403-1` / `COMPARISON-404-1` | `COMPARISON_HISTORY_NOT_OWNER` / `COMPARISON_HISTORY_NOT_FOUND`. 프론트는 comparison 에러 코드로 분기 안 함(`shared/api/model/constant/apiErrorCode.ts`) |
| 삭제 방식 | hard delete | soft delete(`deleted_at`) |
| 인증 실패 | `@PrivateAccess`가 가려져 사실상 공개 | 401 |
| 경로 | `/api/v0/kindergarten/...` | `/api/v1/kindergartens/...` — 프론트 전환 필요 |

## 4. 후속

- soft-delete된 히스토리의 물리 삭제(배치·TTL) — 미정
- KD3-469 비교 조회 API를 이 슬라이스로 이동할지 — 현재는 kindergarten 유지(카탈로그 read)
