> 생성: 2026-09-02 19:24 · 최종 수정: 2026-09-03 00:12

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
- `GET /api/v1/breeds`는 인증 없이 공개(permitAll)한다. 품종 조회는 로그인 여부와 무관하며, 회원가입·반려견 등록 등 비로그인 화면에서도 필요하다.
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

## 완료 확인 기준

- Flyway가 빈 로컬 DB에 카탈로그 스키마와 385건 시드를 정상 적용한다.
- v1 UTF-8 시드의 385건과 v3 Flyway 시드가 전 필드에서 일치하며, 한글·외국어 특수문자가 UTF-8 DB 데이터로 손상 없이 저장된다.
- 전체 조회는 표시 순서를 지키고, 검색은 한글명·개별 별칭 부분 일치와 시작 일치 우선·한글명 가나다순 규칙을 지킨다.
- `GET /api/v1/breeds`는 `id`, `nameKo`, `alias` 항목과 공통 응답 계약을 지킨다.
- 카탈로그 조회와 검색의 단위·통합 테스트를 통과한다.
- API 계약·데이터 인벤토리 영향 문서를 판정하고 결과를 기록한다.

## 검증 결과

- `./gradlew ktlintCheck test --tests 'com.petcampus.knockdog.domain.breed.*' --tests 'com.petcampus.knockdog.HexagonalArchitectureTest'`를 통과했다. 조회 서비스의 공백 처리, JPA 검색 우선순위·정렬, 전체 조회 순서와 아키텍처 경계를 확인했다.
- CP949 CSV를 UTF-8로 변환해 만든 Flyway 시드가 385건인지 확인했고, 믹스견·기타·별칭의 한글 문자열을 대조했다. 이후 CP949 원본에 `?`로 대체된 영어 특수문자 8건이 발견되어, 이 결과만으로는 외국어 문자 보존을 확인할 수 없었다.
- v1 UTF-8 시드와 수정된 V3 시드를 `display_order`, FCI 번호, 영문명, 국문명, 별칭 전체 385행으로 대조해 일치함을 확인했다. V3 파일은 UTF-8로 유효하며 SQL 문자열 값에 `?` 대체 문자가 없다.
- `BreedSeedEncodingTest`로 385행 수, 대체 문자 부재, v1에 있던 외국어 특수문자 8건을 회귀 검증했고, `./gradlew ktlintCheck test --tests 'com.petcampus.knockdog.domain.breed.*' --tests 'com.petcampus.knockdog.HexagonalArchitectureTest'`를 통과했다.
- 로컬 MySQL `knockdog`에서 기존 `breeds`와 V3 Flyway 이력을 제거한 뒤 앱을 재기동해 수정된 V3의 재적용을 확인했다. Flyway는 V3을 정상 적용했고, `breeds`는 385행이다. 8개 특수문자 값의 DB `HEX(name_en)`은 v1 UTF-8 기준값과 전부 일치했다.
- 로컬 서버(9090)에서 `GET /api/v1/breeds`는 `SUCCESS`와 385건(첫 믹스견, 마지막 기타)을 반환했고, `query=휘펫`은 별칭 검색으로 `휘핏`을 반환했다.
- 로컬 MySQL 8.0 `knockdog` DB에서 Flyway V3 적용을 확인했다. 최초 시드 생성 파일에 잘못 들어간 선행 `+` 문자로 실패 이력이 발생했으나, SQL을 수정하고 `flyway repair` 후 V3 적용 성공을 확인했다. 실패는 첫 SQL 문에서 발생해 `breeds` 테이블이 남지 않았다.
- 기동한 로컬 서버(9090)에서 전체 목록과 `query=휘펫` 별칭 검색 응답을 확인했다. 응답은 UTF-8 한글 문자열을 보존했고, 별칭 검색은 `휘핏`을 반환했다.
- 독립 리뷰에서 발견된 permitAll 근거 누락과 검색 공백 처리 불일치를 반영했다. `SecurityConfig`에 `/api/v1/breeds` 공개 근거 주석을 추가하고, `BreedQueryService`는 검색어의 모든 공백을 제거하도록, `BreedJpaRepository`는 JPQL `replace(breed.nameKo, ' ', '')`/`replace(breed.alias, ' ', '')`로 한글명·별칭의 공백도 무시하고 비교하도록 변경했다. `골든 리트리버`를 `골든리트리버`로 검색해도 찾는 케이스를 `BreedJpaRepositoryTest`·`BreedQueryServiceTest`에 추가했고, `./gradlew ktlintCheck test --tests 'com.petcampus.knockdog.domain.breed.*' --tests 'com.petcampus.knockdog.HexagonalArchitectureTest'`를 통과했다.

## 작업 후 확인 목록

| 대상 | 판정 | 근거 |
|---|---|---|
| `docs/work/KD3-418-breed-catalog-v1-api.md` | 갱신 | 작업 결정·검증 결과 기록 |
| `docs/inventory/api.md` | 갱신 | `GET /api/v0/breed-catalog` 판정을 `REDESIGN`·`진행중`으로 갱신하고 v1 구현 티켓 링크 추가 (커밋 `6198768`) |
| `docs/inventory/database.md` | 갱신 | `breed_catalog` 행을 신규 `breeds` 기준 테이블로 갱신, 진척을 `진행중`으로 반영 (커밋 `6198768`) |
| `docs/domains/pet.md` | 갱신 | 견종 기준 데이터 소유·식별자·노출 순서·검색 규칙·공개 API를 신규 추가 (커밋 `6198768`) |
| Notion API 명세서 (`GET /api/v1/breeds`) | 미착수 — 사용자 직접 처리 | 신규 공개 API. 이 세션 환경에 `API_NOTION_KEY`가 없어 AI가 생성 불가, 사용자가 `docs/rules/notion-api-spec-sync.md` 절차로 직접 등록하기로 함 |
