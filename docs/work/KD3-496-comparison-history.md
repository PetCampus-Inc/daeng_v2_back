> 생성: 2026-09-11 10:00 · 최종 수정: 2026-09-11 13:30

# KD3-496 유치원 비교 히스토리 저장·조회·삭제

| 항목 | 값 |
|---|---|
| Jira | `KD3-496` |
| 브랜치 | `feat/KD3-496-comparison-history` |
| 상위 에픽 | `KD3-272` (유치원 도메인 마이그레이션) |

## 현재 제어점

- 활성 workflow: `003-migration`
- 현재 공통 단계: `5` (독립 리뷰·PR·문서 동기화)
- 다음 결정 또는 전환 조건: 독립 리뷰 반영 → PR → epic 머지

## 작업 목표

레거시 `comparison` 도메인의 비교 히스토리 3개 흐름을 신규 서버 `v1`으로 이관한다.

| | 레거시 | v1 |
|---|---|---|
| 저장 | `GET /api/v0/kindergarten/comparisons`의 부수효과(로그인 시) | `GET /api/v1/kindergartens/comparisons`(KD3-469) 컨트롤러가 로그인 시 `KindergartensComparedEvent` 발행 → comparison의 `@EventListener`가 저장 |
| 조회 | `GET /api/v0/kindergarten/comparisons/history?limit=10` `@PrivateAccess` | `GET /api/v1/kindergartens/comparisons/history?limit=10` (인증 필수) |
| 삭제 | `DELETE /api/v0/kindergarten/comparisons/history/{historyId}` `@PrivateAccess` | `DELETE /api/v1/kindergartens/comparisons/history/{historyId}` (인증 필수) |

인벤토리 판정: 조회·삭제는 `REDESIGN`(0004 — `@PrivateAccess`가 permit 규칙에 가려짐).

### 배경

- KD3-469에서 비교 조회 API를 이관하면서 저장 부수효과는 이 티켓으로 분리했다. KD3-469 컨트롤러는 현재 저장을 하지 않는다.
- 레거시 `ComparisonHistory` 엔티티: `id`, `userId`(String, 토큰 subject), `kindergartenIds`(TEXT, CSV), `comparedAt`(LocalDateTime). 테이블 `comparison_history`.
- 레거시 `ComparisonHistoryService`:
  - 저장 — 유저의 모든 히스토리를 조회해 `HashSet(kindergartenIds)` 동등성으로 중복을 찾고, 있으면 `comparedAt`만 갱신, 없으면 새 행. **저장 개수 상한·프루닝 없음.**
  - 조회 — `findByUserIdOrderByComparedAtDesc(userId, PageRequest.of(0, limit))`. 각 히스토리의 유치원 ID를 Redis에서 다시 조회해 `{id, name, thumbnailS3Key, categories}` 요약으로 변환하며, **없어진 유치원(null)은 필터링**한다. `limit`은 컨트롤러 `@RequestParam(defaultValue = "10")`.
  - 삭제 — `findById` → 소유자 확인(`COMPARISON-403-1`) → 없으면 `COMPARISON-404-1` → hard delete.
- 프론트(`daeng_v2_front`):
  - `getComparisonHistory()` — 인자 없음(`limit` 미전송) → `ComparisonHistoryItem[]` = `{ id: number, kindergartens: KindergartenShortInfo[], comparedAt: number[] }`.
  - `KindergartenShortInfo` = `{ id, name, thumbnailS3Key, categories }`.
  - `ComparisonHistoryCard` — `kindergartens: [left, right]`(정확히 2)로 구조분해, `id`/`name`/`thumbnailS3Key`/`categories`만 렌더(`comparedAt` 미사용). `if (!left || !right) return null` — 유치원이 하나라도 없으면 카드 자체를 숨긴다.
  - **`HistoryTab`(`widgets/save-tabs/ui/HistoryTab.tsx`) — `comparedAt`을 `number[]`로 구조분해해(`formatDate([year, month, day])`) 날짜별 그룹 헤더·정렬 키로 쓴다.** ISO 문자열이 오면 `[year, month, day] = "2026-09-11T10:00:00"` → `"2"."0"."2"`로 깨진다. 이력 탭의 날짜 그룹핑이 KD3-495 배열→ISO 전환 전까지 동작 불능.
  - `deleteComparisonHistory(id: number)` — 확인 다이얼로그 없음.

### 패키지 배치 — `domain/comparison` 신규 슬라이스

memo 도메인(KD3-465)의 분리 근거가 히스토리에 그대로 적용된다: 유저 소유 프라이빗 기록, `user_code` 키, 자체 테이블·수명주기, 인가 = 소유자만, 레거시도 별도 패키지. KD3-469 비교 조회 API는 공개 카탈로그의 read 투영이라 `domain/kindergarten`에 그대로 둔다(소유·쓰기·인가가 없음). 히스토리만 `domain/comparison`으로 뺀다.

유일한 차이: 히스토리 응답은 유치원 요약이 필요하다. `comparison` 슬라이스가 `LoadComparisonKindergartenSummariesPort`(가칭)를 정의하고, 어댑터가 kindergarten `LoadKindergartenPort.findByNaverPlaceIds`를 경유한다(KD3-469 `ComparisonAddressAdapter` → auth 패턴과 동일).

### 유저 식별 — `user_code`

KD3-465 memo/checklist가 확립한 패턴을 따른다: `@AuthenticationPrincipal`의 토큰 subject(`UserCode`, 8자)를 `user_code VARCHAR(8)` 컬럼에 그대로 저장한다. `UserId`로 변환하지 않는다.

### dedup — 정렬 저장

레거시는 `HashSet` 동등성으로 `[A,B]`와 `[B,A]`를 같은 히스토리로 본다. v2는 두 ID를 사전순 정렬해 `kindergarten_id_a`(작은 값) / `kindergarten_id_b`(큰 값)로 저장하고 `UNIQUE(user_code, kindergarten_id_a, kindergarten_id_b)`를 건다. 재비교 시 upsert로 `updated_at`만 갱신된다.

응답 `kindergartens` 배열의 순서는 정렬 순서를 따른다 — 프론트가 비교했던 좌/우 순서와 다를 수 있으나, `ComparisonHistoryCard`는 두 유치원을 대등하게 보여주고 카드 클릭 시 `ids`로 다시 조회하므로 표시 순서는 무의미하다.

### `comparedAt` — `updated_at` 재사용

별도 `compared_at` 컬럼을 두지 않고 `BaseEntity.updatedAt`을 comparedAt으로 쓴다(memo 도메인이 `updatedAt.toLocalDate()`를 memoDate로 쓰는 것과 동일). upsert 시 JPA Auditing이 자동 갱신한다.

응답 필드 `comparedAt`은 KD3-495 규약대로 `"2026-09-11T10:00:00"` ISO 문자열이다. 레거시는 `number[]` 배열이었다. `ComparisonHistoryCard`는 이 필드를 안 쓰지만 **`HistoryTab`이 날짜 그룹핑에 `number[]`로 쓰므로, 프론트가 배열→ISO 파싱으로 전환해야 이력 탭이 정상 동작한다**(§미결 질문, 프론트 협의).

## 작업 범위

1. **`domain/comparison` 슬라이스 생성** — `domain`/`application`/`adapter` 표준 헥사고날 구조.
2. **Flyway `V13__create_comparison_histories.sql`**

   ```sql
   CREATE TABLE comparison_histories (
     id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
     user_code          VARCHAR(8)   NOT NULL,
     kindergarten_id_a  VARCHAR(100) NOT NULL,
     kindergarten_id_b  VARCHAR(100) NOT NULL,
     created_at         DATETIME(6)  NOT NULL,
     updated_at         DATETIME(6)  NOT NULL,
     deleted_at         DATETIME(6),
     UNIQUE (user_code, kindergarten_id_a, kindergarten_id_b)
   );
   CREATE INDEX idx_comparison_histories_user_code_updated_at ON comparison_histories (user_code, updated_at);
   ```

3. **도메인** — `ComparisonHistory`(private 생성자 + `create`/`reconstitute`, 두 ID를 정렬해 보관), `ComparisonHistoryId`(value class).
4. **포트** — `SaveComparisonHistoryPort`, `LoadComparisonHistoryPort`(`findRecentByUserCode(userCode, limit)`, `findById(id)`), `LoadComparisonKindergartenSummariesPort`(`findByNaverPlaceIds(ids) → List<ComparisonKindergartenSummary>`).
5. **use case** — `SaveComparisonHistoryUseCase`(`save(userCode, naverPlaceIds)`), `GetComparisonHistoriesUseCase`(`list(userCode, limit)`), `DeleteComparisonHistoryUseCase`(`delete(userCode, historyId)`).
6. **service** — `ComparisonHistoryService`. 저장: 두 ID 정렬 후 upsert. 조회: `findRecentByUserCode` → 유치원 요약 조회 → null 필터. 삭제: `findById` → 소유자 확인 → soft delete.
7. **컨트롤러** — `ComparisonHistoryController`. `GET /api/v1/kindergartens/comparisons/history?limit=10`(기본 10), `DELETE /api/v1/kindergartens/comparisons/history/{historyId}`. `@AuthenticationPrincipal userCode: String`(non-null).
8. **에러 코드** — `ComparisonHistoryErrorCode` — `NOT_FOUND`(404), `NOT_OWNER`(403).
9. **KD3-469 컨트롤러 수정 + 이벤트** — `kindergarten/application/event/KindergartensComparedEvent`(kindergarten이 타입만 정의). `KindergartenComparisonController.compare`가 `principal`(userCode)이 있으면 `compare()` 이후 이벤트 발행. `comparison/adapter/inbound/event/ComparisonHistoryRecorder`(`@EventListener`, 동기)가 받아 `SaveComparisonHistoryUseCase.save`를 호출 — kindergarten이 comparison에 컴파일 의존하지 않는다. 동기 리스너라 저장 예외는 그대로 전파(§미결 질문 2).
10. **요약 조회 어댑터** — `comparison/adapter/outbound/kindergarten/ComparisonKindergartenSummaryAdapter` — kindergarten `LoadKindergartenPort.findByNaverPlaceIds` 경유, `Kindergarten` → `ComparisonKindergartenSummary`(id/name/thumbnailS3Key/categories) 매핑.
11. **어댑터 outbound persistence** — `ComparisonHistoryJpaEntity`(BaseEntity 상속), `ComparisonHistoryJpaRepository`, `ComparisonHistoryPersistenceAdapter`, upsert 쿼리.
12. **문서** — `docs/inventory/api.md` comparison 2개 행(`history`, `history/{id}`) 이관 진척 갱신, `docs/domains/`에 comparison 관련 절 추가.

## 작업 제외 범위

- 저장 개수 상한·프루닝 — 레거시대로 무제한. 필요해지면 후속.
- soft-delete된 히스토리의 물리 삭제(배치) — 후속.
- 프론트 코드 수정 — 이 티켓에서 하지 않는다. `comparedAt` 배열 → ISO 문자열, `v0` → `v1` 경로 전환은 프론트 협의.
- 히스토리에서 "다시 비교" 시 재조회 — 프론트가 `ids`로 KD3-469 API를 다시 호출(이미 그렇게 동작).
- KD3-469 비교 조회 API를 `domain/comparison`으로 이동 — 이번 범위 밖(카탈로그 read라 kindergarten 유지).

## 방향 논의 및 결정 사항

### 확정 사항

- 히스토리는 `domain/comparison` 신규 슬라이스(memo 선례). KD3-469 비교 조회는 kindergarten 유지.
- 유저 식별은 `user_code`(KD3-465 패턴).
- dedup은 정렬된 `kindergarten_id_a`/`_b` + `UNIQUE(user_code, a, b)`. `comparedAt`은 `updated_at` 재사용.
- 저장 트리거는 이벤트 — `KindergartenComparisonController`가 `KindergartensComparedEvent` 발행, comparison의 `@EventListener`가 저장. kindergarten은 이벤트 타입만 정의하고 comparison에 컴파일 의존하지 않는다(comparison → kindergarten 단방향). compare 서비스는 read-only 유지.
- 보존은 레거시대로 무제한. 조회는 `?limit=` optional, 기본 10.
- 삭제는 `deleted_at` soft delete. 소유자 아니면 403, 없으면 404.
- 인증: 조회·삭제 엔드포인트를 `PUBLIC_ENDPOINTS`에 넣지 않아 `authenticated()` 적용.

### 미결 질문

1. **`comparedAt` 응답 포맷** — **해소**: KD3-495 규약대로 ISO 문자열. `ComparisonHistoryCard`는 안 쓰지만 `HistoryTab.tsx`가 `number[]`로 날짜 그룹핑에 쓴다 → 프론트가 배열→ISO 파싱 전환 필요(협의 항목). 서버는 규약을 따른다.
2. **저장 실패 처리** — **해소**: 전파(레거시와 동일). 저장 실패를 비교 조회 응답 뒤에 숨기지 않는다.
3. **`limit` 상한** — **해소**: 상한 50. `limit <= 0`이면 기본 10. 악의적 큰 값 방지.
4. **없어진 유치원이 포함된 히스토리** — **해소**: 레거시 동작 유지 — 요약 배열에서 null 필터링, 히스토리 자체는 응답에 남긴다(프론트가 카드를 숨김). 이력을 임의로 소실시키지 않는다.

### 사용자 승인 기록

- 2026-09-11 — 설계안(comparison 슬라이스 / 레거시대로 보존·limit / 컨트롤러 조립) 승인("진행해줘").
- 2026-09-11 — 독립 리뷰 지적 반영 후 PR 진행.

### 독립 리뷰

컨텍스트 없는 리뷰어가 커밋 `08818b4..ec46bb6`와 이 문서를 대조(2026-09-11). 정합성·아키텍처(ArchUnit)·컨벤션·커밋 전부 이상 없음, 블로커 없음.

| 지적 | 처리 |
|---|---|
| `comparedAt` 프론트 영향 과소평가 — `ComparisonHistoryCard`는 안 쓰지만 `HistoryTab.tsx`가 `number[]`로 날짜 그룹핑에 씀(ISO로 오면 `"2.00.02"`로 깨짐) | §배경·§`comparedAt`·대조표·`comparison.md` §3 전부 `HistoryTab.tsx` 명시로 수정 |
| 네이티브 `upsert`(`ON DUPLICATE KEY`)의 핵심 동작(재비교 시 새 행 안 생김, soft delete 되살아남)이 테스트 안 됨 | `ComparisonHistoryEndpointTest`에 2개 추가 |
| `ComparisonKindergartenSummaryAdapter` 단위 테스트 없음 | `ComparisonKindergartenSummaryAdapterTest` 추가 |
| `KindergartenComparisonEndpointTest`의 `comparisonHistoryJpaRepository.deleteAll()`이 테스트마다 인라인 | `@BeforeEach`로 통일 |

## 완료 확인 기준

### 테스트 (2026-09-11, `./gradlew clean test ktlintCheck` — 총 227개, 실패 0, ArchUnit 통과)

- `ComparisonHistoryTest` (3) — 두 ID 사전순 정렬 불변식, `naverPlaceIds` 순서, 같은 ID 두 개 거부.
- `ComparisonHistoryServiceTest` (8) — 저장 시 ID 정렬 upsert, 2곳 아니면 거부, `limit` 1~50 clamp, 히스토리별 유치원 요약, 없어진 유치원 필터(행은 유지), 삭제 NOT_FOUND/NOT_OWNER/soft delete.
- `ComparisonHistoryEndpointTest` (7, `@SpringBootTest`+MockMvc) — 비로그인 401, 최근순 조회, 삭제 후 목록에서 사라짐, 남의 것 403 `COMPARISON_HISTORY_NOT_OWNER`, 없는 것 404 `COMPARISON_HISTORY_NOT_FOUND`, **네이티브 upsert 검증**(같은 쌍을 순서 바꿔 재비교 → 새 행 안 생기고 `updated_at`만 전진, soft delete 후 재비교 → `deleted_at = NULL`로 되살아나 목록에 다시 보임).
- `ComparisonKindergartenSummaryAdapterTest` (2) — 빈 목록 가드, 존재하는 유치원만 요약 변환.
- `KindergartenComparisonEndpointTest` (+2, `@BeforeEach`로 `comparison_histories` 정리) — 로그인 비교 시 정렬된 쌍 기록, 비로그인 비교는 미기록.

### REDESIGN 응답 대조 (`003-migration.md` §4)

레거시 서버(Redis 의존)를 로컬에서 못 띄워 소스 대조로 갈음. 레거시 `ComparisonHistoryResponse.java` / `ComparisonHistoryService.java` / `ComparisonController.java` 대비:

| 항목 | 레거시 | v1 | 차이 |
|---|---|---|---|
| `id` | 히스토리 PK(Long) | 〃 | 동일 |
| `kindergartens[]` | `{id, name, thumbnailS3Key, categories}`, 없어진 유치원 필터 | 〃 | 동일. 요약은 Redis → RDB(`LoadKindergartenPort`) |
| `comparedAt` | `number[]` (`[2026,9,11,10,0,0]`) | `"2026-09-11T10:00:00"` ISO | **포맷 변경 — 의도(KD3-495).** `ComparisonHistoryCard`는 미사용이나 `HistoryTab.tsx`가 날짜 그룹핑에 `number[]`로 씀 → 프론트 파싱 전환 필요 |
| 정렬 | `comparedAt DESC` | `updated_at DESC, id DESC` | `updated_at`이 comparedAt이므로 동등 |
| `limit` | `@RequestParam(defaultValue="10")`, 상한 없음 | 기본 10, 1~50 clamp | 상한 추가 — 악의적 큰 값 방지 |
| 저장 dedup | 유저 전체 히스토리 로드 후 `HashSet` 동등성 | 정렬 저장 + `UNIQUE` upsert | 결과 동일, 쿼리 효율 개선 |
| 삭제 | hard delete | soft delete(`deleted_at`) | **의도.** 재비교 시 `deleted_at = NULL`로 되살아남 |
| 삭제 에러 | `COMPARISON-403-1` / `COMPARISON-404-1` | `COMPARISON_HISTORY_NOT_OWNER` / `COMPARISON_HISTORY_NOT_FOUND` | code 문자열 다름 — 프론트는 comparison 에러 코드로 분기 안 함 |
| 인증 실패 | `@PrivateAccess`가 permit에 가려져 사실상 공개(0004) | 401 | **보안버그 교정** |
| 삭제 응답 본문 | `Response.success()` (data null) | 〃 | 동일(`responseTime` 제외, KD3-258 선례) |

**프론트 협의**: `comparedAt` 배열 → ISO 문자열, `v0` → `v1` 경로.

## 작업 후 확인 목록

- `docs/inventory/api.md` — `comparisons/history` GET·DELETE 행 `미착수` → `진행중`.
- `docs/inventory/database.md` — `comparison_history` 행 `DEFER`/`미착수` → 판정·진척 갱신(테이블명 `comparison_histories`, 보존 무제한).
- `docs/domains/` — comparison 관련 절 추가(별도 `comparison.md` 또는 `kindergarten.md` 확장, 배치는 `documentation.md` 기준으로 판정).
- `docs/conventions/*` — 해당 없음.
