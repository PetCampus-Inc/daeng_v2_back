> 생성: 2026-09-02 19:24 · 최종 수정: 2026-09-04 13:41

# KD3-418 견종 카탈로그 v1 API 구축

| 항목 | 값 |
|---|---|
| Jira | `KD3-418` |
| 브랜치 | `feat/KD3-418-breed-catalog-v1-api` |
| 상위 에픽 | `KD3-404` |

## 현재 제어점

- 활성 workflow: `003-migration`, `005-new-feature`
- 현재 공통 단계: `5`
- 독립 리뷰에서 permitAll 근거 미기록·검색 공백 처리 불일치를 발견해 반영 완료. Notion API 명세 동기화는 사용자가 직접 처리하기로 함.
- PR: [#14](https://github.com/PetCampus-Inc/daeng_v2_back/pull/14) (`feat/KD3-418-breed-catalog-v1-api` → `epic/KD3-404-pet-domain-migration`)
- 다음 결정 또는 전환 조건: 머지 직전 작업 문서·PR 본문·코드 최종 대조.

## 작업 목표

견종 카탈로그를 신규 서버의 단일 기준 데이터로 구축하고, 프론트 전환 대상인 `GET /api/v1/breeds` 조회 API를 제공한다.

## 작업 범위

- `breeds` Flyway 스키마와 검증 가능한 385건 시드를 추가한다. 컬럼은 `id`, `display_order`, `fci_standard_number`, `name_en`, `name_ko`, `alias`로 한다.
- 한글명·별칭 검색과 제품 정의 표시 순서를 제공한다. `display_order` 1번은 믹스견, 마지막은 기타로 고정한다.
- `GET /api/v1/breeds?query=`를 구현한다.
- 이후 `pets.breed_id`가 참조할 persistence 모델과 조회 포트를 제공한다.

## 작업 제외 범위

- 레거시 `/api/v0/breed`, `/api/v0/breed-catalog` 호환 API
- 운영 데이터 backfill·cutover
- 카탈로그를 관리자가 수정하는 기능

## 방향 논의 및 결정 사항

### 확정 사항

- 신규 API는 v1 REST 경로를 사용한다.
- `breeds`는 pet의 견종 값에 대한 단일 기준이며, pet은 견종명을 중복 저장하지 않고 `breed_id`를 참조한다.
- v0는 신규 서버에 구현하지 않는다. v1 API만 제공하며, v0 제공과 실제 프론트 전환은 이번 티켓 범위가 아니다.
- `display_order`는 DB 한글 정렬에 의존하지 않는 제품 고정 노출 순서다. 전체 목록은 이 순서로 조회한다.
- `fci_standard_number`는 내부 목록 관리용이며 서비스 동작에 사용하지 않는다. FCI 번호가 없는 믹스견과 기타는 NULL을 허용한다.
- `name_en`과 `name_ko`에는 FCI 등록 품종의 공식 영문·국문 명칭을 저장한다. 믹스견과 기타에는 제품이 정한 명칭을 저장한다.
- `alias`는 공식 명칭 외 검색을 돕는 품종당 단일 별칭이며 NULL을 허용한다.
- 목록 응답 항목은 `id`, `nameKo`, `alias`를 제공한다. 화면은 별칭이 있을 때만 `한글명 (별칭)`으로 조합한다.
- `query`는 검색어와 비교 대상(한글명·별칭) 양쪽 모두에서 모든 공백을 제거한 뒤 부분 일치로 검색한다. `name_ko`에 공백이 포함된 다단어 품종명(예: `골든 리트리버`)이 많아, 공백 없이 입력해도(`골든리트리버`) 찾을 수 있어야 하기 때문이다. 검색 결과는 검색어로 시작하는 항목을 먼저, 시작하지 않지만 포함하는 항목을 다음으로 정렬하고, 각 동순위에서는 한글명 가나다순으로 정렬한다. 검색 결과가 없으면 빈 목록을 반환한다.
- `GET /api/v1/breeds`는 인증 없이 공개(permitAll)한다. 품종 조회는 로그인 여부와 무관하며, 회원가입·반려견 등록 등 비로그인 화면에서도 필요하다. 이 근거는 `docs/conventions/code-style.md`(KD3-413, 주석 금지)의 merge 이후 코드 주석이 아니라 이 문서와 PR 본문에만 남긴다.
- `fciStandardNumber`/`fci_standard_number`, `nameEn`·`nameKo`/`name_en`·`name_ko`는 `code-style.md`의 약어 금지 규칙 문면상 후보로 검토했으나, 누가 봐도 뜻이 분명한 이름이라 그대로 유지한다.
- 검색어의 `%`/`_`가 SQL `LIKE` 와일드카드로 해석되던 문제를 발견해, `BreedPersistenceAdapter`에서 검색어의 `\`/`%`/`_`를 이스케이프하고 `BreedJpaRepository`의 JPQL에 `escape '\'`를 추가하도록 수정했다. 이스케이프는 애플리케이션 계층(`BreedQueryService`)이 아니라 SQL 구현 세부사항을 다루는 영속성 어댑터에 둔다.
- 시드의 단일 기준은 `daeng_v1_back/scripts/migrations/KD3-370-create-breed.sql`의 UTF-8 385건이다. v3 Flyway SQL은 이 데이터를 `name_en`, `name_ko` 컬럼 순서로 옮긴다.
- `docs/work/똑독_견종목록_2026-08-11.csv`는 CP949 파일이며, 스웨덴·스페인어 등의 특수문자가 `?`로 이미 대체돼 있다. 출처 확인용으로만 보관하며 시드 생성의 기준으로 사용하지 않는다.
- 운영 데이터 backfill·cutover·rollback은 신규 기준 데이터와 v1 API 추가만 하는 이번 작업에 해당하지 않는다.

### 미결 질문

- 없음.

### 사용자 승인 기록

- 2026-09-02: 사용자가 견종 카탈로그 ID 참조와 신규 v1 REST API 전략을 승인했다.
- 2026-09-02: 사용자가 v0 신규 구현 제외, v1 단독 제공, 신규 스키마의 `id` PK와 컬럼 의미, 표시·검색 규칙, CSV 원본 사용을 확정했다.
- 2026-09-02: 사용자가 v1 UTF-8 시드를 기준으로 v3의 전수 데이터 정합성을 복구하고, 문자 손상 회귀 검증을 추가하는 것을 승인했다.
- 2026-09-02: 독립 리뷰에서 `GET /api/v1/breeds` permitAll 결정이 작업 문서에 기록되지 않은 것을 발견했다. 사용자가 공개 엔드포인트로 확정하고 근거를 기록하도록 승인했다.
- 2026-09-02: 독립 리뷰에서 검색 공백 처리 문구("공백을 제거해")와 구현(앞뒤 trim만)의 불일치를 발견했다. 사용자가 시드 내 다단어 품종명 확인 후, 검색어·비교 대상 양쪽 모두 공백을 제거하는 방식으로 구현을 변경하도록 승인했다.
- 2026-09-03: `epic/KD3-404-pet-domain-migration`을 merge하며 `docs/conventions/code-style.md`(KD3-413, 주석 금지·약어 금지·DTO-도메인 일치)가 새로 유입됐다. 대조 결과 breed 도메인 코드 자체는 위반이 없었고, `SecurityConfig.kt`에 이번 티켓에서 새로 추가한 주석 2줄만 위반이었다. 사용자가 그 주석을 제거하고 근거는 작업 문서·PR 본문에만 남기도록, `fciStandardNumber`/`nameEn`/`nameKo` 계열 약어 후보는 의미가 명확하다는 이유로 그대로 유지하도록 확정했다.
- 2026-09-03: 사용자가 이 문서의 검증 결과 서술이 실제 코드 상태(주석 추가↔제거 모순)와 어긋나고, Gradle·Flyway·로컬 서버 검증이 PR diff에서 확인 불가능한 점을 지적했다. 검증 결과를 CI 로그·이 세션의 재현 결과로 다시 정리하고, 재현하지 못한 항목(v1 대 V3 전 필드 대조)은 확인 불가로 표시했다.
- 2026-09-03: 검색어의 `%`/`_`가 이스케이프 없이 `LIKE`에 바인딩돼 와일드카드로 해석되는 문제가 지적됐다. SQL 인젝션은 아니고(파라미터 바인딩 사용) 385건 고정 공개 데이터라 심각도는 낮지만, 수정 비용이 낮아 사용자가 수정과 회귀 테스트 추가를 승인했다.
- 2026-09-03: `BreedSeedEncodingTest`가 `String(bytes, UTF_8)` 디코딩 중 잘못된 바이트가 U+FFFD로 조용히 치환돼도 통과한다는 지적이 있었다 — 기존 정규식은 ASCII `?`만 잡고 U+FFFD는 잡지 못했다. 현재 파일이 손상됐다는 뜻은 아니지만(이 세션에서 HEX 대조로 이미 확인) 이 테스트의 존재 목적과 직결되는 커버리지 구멍이라, 사용자가 정규식 확장과 탐지 자체를 증명하는 양성 케이스 테스트 추가를 승인했다.
- 2026-09-04: KD3-430(pet 도메인 기반 스키마) 착수 과정에서 `docs/domains/pet.md`를 `docs/domains/auth.md`(기준 형식)와 대조한 결과, 컬럼 단위 제약(`display_order` UNIQUE, `fci_standard_number`·`alias`의 nullable 의미, `name_en`/`name_ko`의 FCI 공식명 대 제품 정의 명칭 구분)과 참조 섹션이 빠져 있는 것을 발견했다. 머지 전 문서이므로 사용자가 이 KD3-418 브랜치에서 직접 보완하도록 확정했다.

## 완료 확인 기준

- Flyway가 빈 로컬 DB에 카탈로그 스키마와 385건 시드를 정상 적용한다.
- v1 UTF-8 시드의 385건과 v3 Flyway 시드가 전 필드에서 일치하며, 한글·외국어 특수문자가 UTF-8 DB 데이터로 손상 없이 저장된다.
- 전체 조회는 표시 순서를 지키고, 검색은 한글명·개별 별칭 부분 일치와 시작 일치 우선·한글명 가나다순 규칙을 지킨다.
- `GET /api/v1/breeds`는 `id`, `nameKo`, `alias` 항목과 공통 응답 계약을 지킨다.
- 카탈로그 조회와 검색의 단위·통합 테스트를 통과한다.
- API 계약·데이터 인벤토리 영향 문서를 판정하고 결과를 기록한다.

## 검증 결과

- **CI**: `build` 워크플로(`./gradlew build --no-daemon` — ktlint, 컴파일, `BreedQueryServiceTest`·`BreedJpaRepositoryTest`·`BreedSeedEncodingTest`·`HexagonalArchitectureTest`를 포함한 전체 테스트)가 PR #14 HEAD(`1ebcf85`) 기준으로 통과했다: https://github.com/PetCampus-Inc/daeng_v2_back/actions/runs/33712118131/job/100513664365
- **Flyway 재적용 (2026-09-03, 이 세션에서 재현)**: 로컬 MySQL(`docker-compose.local.yaml`)의 `breeds` 테이블과 V3 `flyway_schema_history` 행을 삭제해 빈 상태로 되돌린 뒤 `./gradlew bootRun --args='--spring.profiles.active=local'`로 재기동했다. 로그에 `Migrating schema knockdog to version "3 - create breeds"` → `Successfully applied 1 migration to schema knockdog, now at version v3`가 남았고, 적용 후 `SELECT COUNT(*) FROM breeds`는 385였다.
- **문자 인코딩 (2026-09-03, 이 세션에서 재현)**: 위 상태의 DB에서 `SELECT name_en, HEX(name_en) FROM breeds WHERE name_en REGEXP '[^ -~]'`로 CP949 원본에서 `?`로 대체됐던 영어 특수문자 8건(`SMÅLANDSSTÖVARE` 등)을 직접 조회했다. 전부 정상 UTF-8 hex(`Å`=`C385`, `Ö`=`C396`, `Ä`=`C384`, `Ü`=`C39C`, `Á`=`C381`, `Ç`=`C387`)였고 `?`(0x3F) 대체 문자는 없었다. `BreedSeedEncodingTest`가 같은 조건을 회귀 테스트로 고정한다.
- **로컬 API (2026-09-03, 이 세션에서 재현)**: 같은 서버에 인증 헤더 없이 요청해 확인했다 — `GET /api/v1/breeds` → `code: SUCCESS`, 385건, 첫 항목 `id:1 믹스견`, 마지막 항목 `id:385 기타`(alias `목록에 없는 품종`), HTTP 200. `GET /api/v1/breeds?query=골든리트리버`(공백 없이 입력) → `id:4 골든 리트리버` 1건 반환, HTTP 200. 공백 무시 검색과 permitAll이 실제 HTTP 레벨에서 동작함을 확인했다.
- **v1 시드 대 V3 시드 전 필드 대조**: 385건의 `display_order`·FCI 번호·영문명·국문명·별칭이 전부 일치한다는 것을 구현 시점(2026-09-02)과 독립 리뷰(§확정 사항 2026-09-02/03) 두 차례 스크립트로 대조했으나, 두 대조 모두 산출물을 로그·파일로 보존하지 않았다. **확인 불가로 표시한다** — 재현하려면 `daeng_v1_back/scripts/migrations/KD3-370-create-breed.sql`과 `V3__create_breeds.sql`을 다시 스크립트로 대조해야 한다.
- **참고(과거 기록, 재현 대상 아님)**: 최초 시드 생성 시 SQL 값 앞에 잘못 들어간 `+` 문자로 2026-09-02 로컬에서 Flyway 적용이 한 차례 실패했다. SQL을 수정하고 `flyway repair` 후 재적용에 성공했으며, 위 2026-09-03 재현에서는 이 문제 없이 V3이 한 번에 적용됐다.
- **LIKE 메타문자 이스케이프 (2026-09-03)**: `BreedPersistenceAdapterTest`에 `_`, `%`, `\`(이스케이프 문자 자체)를 포함한 검색어 회귀 테스트 3건을 추가해 각각 리터럴로만 매칭되고 와일드카드로 해석되지 않음을 확인했다. 테스트 DB(H2, `MODE=MySQL`)뿐 아니라 실제 로컬 MySQL 8.0에도 동일한 `LIKE ... ESCAPE '\'` 패턴을 직접 실행해 `가나다`(매칭 안 됨)·`가_다`·`가%다`·`가\다`(각 검색어에만 매칭) 결과로 교차 확인했다.
- **인코딩 손상 탐지 정규식 보강 (2026-09-03)**: `BreedSeedEncodingTest`의 `REPLACED_CHARACTER_IN_SQL_VALUE`가 ASCII `?`만 잡고 UTF-8 디코딩 실패 시 나오는 U+FFFD(`�`)는 못 잡던 것을 `[?�]`로 확장했다. 정규식이 실제로 U+FFFD와 `?` 둘 다 탐지하고 정상 문자(`Ä`)에는 반응하지 않는지 증명하는 양성/음성 케이스 테스트를 추가했다.

## 작업 후 확인 목록

| 대상 | 판정 | 근거 |
|---|---|---|
| `docs/work/KD3-418-breed-catalog-v1-api.md` | 갱신 | 작업 결정·검증 결과 기록 |
| `docs/inventory/api.md` | 갱신 | `GET /api/v0/breed-catalog` 판정을 `REDESIGN`·`진행중`으로 갱신하고 v1 구현 티켓 링크 추가 (커밋 `6198768`) |
| `docs/inventory/database.md` | 갱신 | `breed_catalog` 행을 신규 `breeds` 기준 테이블로 갱신, 진척을 `진행중`으로 반영 (커밋 `6198768`) |
| `docs/domains/pet.md` | 갱신 | 견종 기준 데이터 소유·식별자·노출 순서·검색 규칙·공개 API를 신규 추가 (커밋 `6198768`). 이후 확정된 검색 공백 무시 규칙과 permitAll 인증 정책이 누락돼 있던 것을 발견해 추가 반영. 2026-09-04 KD3-430 착수 중 컬럼 제약(`display_order` UNIQUE, nullable 의미, 명칭 출처)과 참조 섹션 누락을 발견해 추가 반영 |
| Notion API 명세서 (`GET /api/v1/breeds`) | 미착수 — 사용자 직접 처리 | 신규 공개 API. 이 세션 환경에 `API_NOTION_KEY`가 없어 AI가 생성 불가, 사용자가 `docs/rules/notion-api-spec-sync.md` 절차로 직접 등록하기로 함 |
