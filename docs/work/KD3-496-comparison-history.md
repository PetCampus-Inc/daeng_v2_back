> 생성: 2026-09-11 10:00 · 최종 수정: 2026-09-11 10:00

# KD3-496 유치원 비교 히스토리 저장·조회·삭제

| 항목 | 값 |
|---|---|
| Jira | `KD3-496` |
| 브랜치 | `feat/KD3-496-comparison-history` |
| 상위 에픽 | `KD3-272` (유치원 도메인 마이그레이션) |

## 현재 제어점

- 활성 workflow: `003-migration`
- 현재 공통 단계: `3` (구현)
- 다음 결정 또는 전환 조건: 구현·검증 완료 → 5단계 독립 리뷰·PR

## 작업 목표

레거시 `comparison` 도메인의 비교 히스토리 3개 흐름을 신규 서버 `v1`으로 이관한다.

| | 레거시 | v1 |
|---|---|---|
| 저장 | `GET /api/v0/kindergarten/comparisons`의 부수효과(로그인 시) | `GET /api/v1/kindergartens/comparisons`(KD3-469) 컨트롤러가 로그인 시 저장 use case 호출 |
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
  - `ComparisonHistoryCard` — `kindergartens: [left, right]`(정확히 2)로 구조분해, `id`/`name`/`thumbnailS3Key`/`categories`만 렌더. **`comparedAt`은 렌더에 쓰지 않는다.** `if (!left || !right) return null` — 유치원이 하나라도 없으면 카드 자체를 숨긴다.
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

응답 필드 `comparedAt`은 KD3-495 규약대로 `"2026-09-11T10:00:00"` ISO 문자열이다. 레거시는 `number[]` 배열이었으나 `ComparisonHistoryCard`가 이 필드를 렌더에 쓰지 않으므로 영향이 작다(§미결 질문).

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
9. **KD3-469 컨트롤러 수정** — `KindergartenComparisonController.compare`가 `principal`(userCode)이 있으면 `compareKindergartensUseCase.compare()` 이후 `saveComparisonHistoryUseCase.save(userCode, ids)`를 호출.
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
- 저장 트리거는 `KindergartenComparisonController`(어댑터)가 조립 — compare 서비스는 read-only 유지, kindergarten→comparison 슬라이스 순환 회피.
- 보존은 레거시대로 무제한. 조회는 `?limit=` optional, 기본 10.
- 삭제는 `deleted_at` soft delete. 소유자 아니면 403, 없으면 404.
- 인증: 조회·삭제 엔드포인트를 `PUBLIC_ENDPOINTS`에 넣지 않아 `authenticated()` 적용.

### 미결 질문

1. **`comparedAt` 응답 포맷** — **해소**: KD3-495 규약대로 ISO 문자열. `ComparisonHistoryCard`가 렌더에 안 써서 영향 작음. 프론트 협의 항목으로 남김.
2. **저장 실패 처리** — **해소**: 전파(레거시와 동일). 저장 실패를 비교 조회 응답 뒤에 숨기지 않는다.
3. **`limit` 상한** — **해소**: 상한 50. `limit <= 0`이면 기본 10. 악의적 큰 값 방지.
4. **없어진 유치원이 포함된 히스토리** — **해소**: 레거시 동작 유지 — 요약 배열에서 null 필터링, 히스토리 자체는 응답에 남긴다(프론트가 카드를 숨김). 이력을 임의로 소실시키지 않는다.

### 사용자 승인 기록

- 2026-09-11 — 설계안(comparison 슬라이스 / 레거시대로 보존·limit / 컨트롤러 조립) 승인("진행해줘").

## 완료 확인 기준

- `ComparisonHistoryService` 테스트 — 저장 시 ID 정렬·upsert(재비교 시 새 행 안 생김), 조회 시 최근순·limit·유치원 요약·null 필터, 삭제 시 소유자 아니면 403·없으면 404·soft delete.
- `ComparisonHistory` 도메인 테스트 — 두 ID 정렬 불변식.
- 컨트롤러 테스트(`@SpringBootTest`+MockMvc) — 비로그인 401, 조회 성공, 삭제 성공/403/404, `GET /comparisons`(KD3-469) 호출 시 로그인이면 히스토리 저장됨.
- `./gradlew clean test ktlintCheck` 통과, ArchUnit 통과.
- **KEEP/REDESIGN 응답 대조** — 레거시 `ComparisonHistoryResponse` / `ComparisonController` 소스와 v1 응답을 필드 단위 대조. 차이(`comparedAt` ISO, 에러 코드 문자열, soft delete)는 의도된 것으로 기록.

## 작업 후 확인 목록

- `docs/inventory/api.md` — `comparisons/history` GET·DELETE 행 `미착수` → `진행중`.
- `docs/inventory/database.md` — `comparison_history` 행 `DEFER`/`미착수` → 판정·진척 갱신(테이블명 `comparison_histories`, 보존 무제한).
- `docs/domains/` — comparison 관련 절 추가(별도 `comparison.md` 또는 `kindergarten.md` 확장, 배치는 `documentation.md` 기준으로 판정).
- `docs/conventions/*` — 해당 없음.
