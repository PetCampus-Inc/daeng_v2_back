> 생성: 2026-09-01 21:05 · 최종 수정: 2026-09-02 17:40

# KD3-413 — 유치원 도메인 스키마 이관 및 정적 조회 기능

| 항목 | 값 |
|---|---|
| Jira | `KD3-413` |
| 브랜치 | `feat/KD3-413-kindergarten-static-lookup` |
| 상위 에픽 | `KD3-272`(Task) — git 브랜치는 `epic/KD3-272-kindergarten-schema` |

## 현재 제어점

- 활성 workflow: `003-migration`
- 현재 공통 단계: `5`(독립 리뷰·PR·문서 동기화) — [PR #12](https://github.com/PetCampus-Inc/daeng_v2_back/pull/12) 생성 완료(`epic/KD3-272-kindergarten-schema` 대상). 리뷰 코멘트 반영 라운드 진행 중(주소 스키마 변경 V4 추가, 시딩 ObjectMapper 버그 수정, 코드 내 설명 주석 전면 제거). 로컬 응답 대조·시딩 데이터 검증(4단계 항목)은 여전히 환경 문제로 미완료 — PR Test plan에 미체크로 남겨뒀다.
- 다음 결정 또는 전환 조건: 리뷰 반영 + 사람이 로컬 응답 대조·시딩 검증을 완료해야 머지 가능(`000-common.md` §5 "문서 갱신과 필수 검증이 끝난 뒤에만 머지를 준비한다").

## 작업 목표

레거시(`daeng_v1_back`)에서 Redis를 source of truth로 쓰던 유치원 정적 정보(요약, 상세, 요금표)를 신규 서버의 MySQL 스키마로 이관하고, 그 위에서 동작하는 조회 API를 구현한다. 완료 시 신규 서버가 DB를 유일한 정답으로 삼아 유치원 요약(헤더)·상세 조회·요금표 조회를 제공하게 된다.

## 작업 범위

### 데이터

- 시딩 기준: `info_new.json` 하나. `dobonggu.json` 등 자치구별 크롤링 파일 25개·`info_and_review.json`은 확인 결과 어떤 코드에서도 읽지 않는 미사용 파일로 보이나, 이번 시딩은 그 흡수 여부를 별도 검증하지 않고 `info_new.json`에 있는 내용을 그대로 기준으로 삼는다(재크롤링은 후속 작업 몫).
- `price_and_product.json`(메뉴/요금)도 대상. `avg_price_per_time.json`은 이번 API 범위에 안 쓰일 가능성이 높아 아래 "미결 질문"에서 재확인하고, `product_pricing.json`은 `comparison` 도메인 소관이라 범위 밖이다.
- 스키마 설계 대상: 유치원 기본 정보(리뷰수·썸네일 포함), 카테고리, 영업시간(다중 프로필), 링크, 옵션(견종/서비스/안전시설/편의시설), 가격표 이미지, 메뉴 — 상세는 아래 "스키마 설계" 참고
- 위 JSON을 파싱해 신규 MySQL 스키마로 시딩하는 절차(스크립트 또는 마이그레이션 데이터)

### 스키마 설계 — 구현 중 실제 JSON 대조로 수정됨

`info_new.json`/`price_and_product.json` 실제 필드를 하나씩 대조한 결과, 화면 캡처만 보고 추정했던 부분 3곳이 실제 크롤링 데이터와 달랐다. 화면 추정이 아니라 실제 크롤링 데이터를 기준으로 스키마를 확정한다.

- **브레이크타임·공휴일 휴무 컬럼 삭제**: `business_hours` JSON엔 `name`/`weekdays{open,close}`/`weekends{open,close}`/`offdays`(예: `["THURSDAY"]`)만 있고 브레이크타임, 공휴일 휴무 필드가 없다. 화면의 "브레이크 타임"은 크롤링 데이터로 채울 수 없어 컬럼을 만들지 않는다(원장이 프로필을 편집하게 되면 그때 추가 — 이번 범위 밖).
- **`kindergarten_gallery_images` 테이블 삭제**: 레거시 `KindergartenMapper`를 보면 `main/{id}`의 `banner`가 `[thumbnail_s3_key] + menu_image_s3_keys`로 합성된다 — 별도 갤러리 이미지 목록이 크롤링 데이터에 없다. `thumbnail_s3_key`는 `kindergartens` 루트 컬럼, 나머지는 `kindergarten_price_images`(아래)로 충분하다.
- **`kindergarten_price_images`의 원본은 `product_pricing.json`이 아니라 `info_new.json`의 `menu_image_s3_keys` 배열**이다(레거시 매퍼에서 확인).
- **`kindergarten_avg_prices` 스키마에서 제외 확정**: 미결이었던 항목을 실제 코드로 재확인한 결과 `main`/`basic`/`pricing` 응답 어디에도 안 쓰이는 게 맞아, 이번 스키마에서 뺀다. 필요해지면 map-view 등 후속 작업에서 추가한다.

```
kindergartens                    루트: naver_place_id, name, address(도로명 주소만), address_detail(상세 주소, nullable),
│                                        lat/lng, phone_number, thumbnail_s3_key,
│                                        source(CRAWLED/OWNER_REGISTERED), status(ACTIVE/CLOSED), visitor_review_count, blog_review_count
├─ kindergarten_categories        1:N — KINDERGARTEN/HOTEL 등 복수 업종 (`categories` 배열)
├─ kindergarten_business_hours    1:N — name(DEFAULT/KINDERGARTEN/HOTEL 프로필 구분), weekday/weekend open·close, offdays(JSON 배열)
├─ kindergarten_links             1:N — code(INSTAGRAM/BLOG/HOMEPAGE/YOUTUBE 등), url (`links` 배열)
├─ kindergarten_options           1:N — option_group(DOG_BREED/DOG_SERVICE/SAFETY_FACILITY/VISITOR_AMENITY) + option_code.
│                                        display_order 컬럼 없음 — 정렬은 option_code 고정 순서(애플리케이션 레벨)를 따른다
├─ kindergarten_price_images      1:N — s3_key, display_order (`menu_image_s3_keys` 배열)
└─ kindergarten_menus             1:N — product_type, service_type, product_name, unit, unit_str, unit_type, weight_range,
                                         price, hourly_price, is_min_price, is_max_price, total_duration_str, total_duration_minutes,
                                         display_order (`price_and_product.json`, kindergarten_id로 join)
```

### API — 정적 조회만

이 서버는 `v1`만 제공한다. `v0`는 이 서버에 만들지 않는다(2026-09-02 최종 결정 — [`ADR 0012`](../adr/0012-신규-서버-v0-미제공-원칙.md), 일반 정책으로 분리):

| Method | Path | 비고 |
|---|---|---|
| GET | `/api/v1/kindergartens/{id}/summary` | 레거시 `main/{id}` 재설계 — 지번 주소는 저장하지 않고 `address`(도로명 주소)/`addressDetail`(상세 주소, nullable)로 분리, `operationStatus`에 `HOLIDAY` 추가 |
| GET | `/api/v1/kindergartens/{id}/detail` | 레거시 `basic/{id}` 재설계 — 실체 없는 `breakTime` 필드 제거 |
| GET | `/api/v1/kindergartens/{id}/pricing` | 레거시 `{id}/pricing`과 응답 모양 동일(고칠 버그 없음), 경로만 `v1` |

기존 `v0`(`main`/`basic`/`pricing`) 호출은 레거시 서버가 계속 담당한다(라우팅/LB 레벨, 이 서버 코드와 무관).

**결정 경위**: "이름이 실제 동작과 다를 때만 v1"(ADR 0011 원안) → "이름 무관 3개 전부 v1, v0도 이 서버에 병행"(1차 변경) → "이 서버는 v0 자체를 만들지 않는다"(최종). 마지막 변경으로 `KindergartenController`(v0)와 v0 전용 응답 DTO 2개(`KindergartenSummaryResponse`/`KindergartenDetailResponse`의 v0판)를 삭제하고, v1 파일들의 `V1` 접미사를 뗐다(예: `KindergartenV1Controller` → `KindergartenController`) — 이제 이 도메인엔 버전 트윈이 없어 접미사가 불필요해졌다.

**`summary`의 `bookmarked`/`memoData` 미제공**: 레거시 `main/{id}`엔 이 두 필드가 있지만 `bookmark`/`memo` 도메인이 이 저장소에 없어(`memo`는 인벤토리에서도 `REDESIGN`/미착수) `summary` 응답엔 아예 넣지 않는다 — 신규 계약이라 스텁 고정값을 먼저 노출할 필요가 없고, 도메인이 생기면 필드를 추가하는 쪽이 자연스럽다.

**거리 계산(`dist`)**: 레거시는 저장된 값이 아니라 요청마다 계산한다(`KindergartenQueryService.calculateDistance`) — 구면 삼각법 기반 근사 공식(위경도 차이 → `acos` → 60 * 1.1515로 해리를 마일로, `* 1.609344`로 km 변환)이고 `lat`/`lng` 쿼리 파라미터가 필수다. 정밀한 Haversine은 아니지만 오차가 크지 않은 근사식이라 그대로 이식하면 된다. 스키마에 저장할 값이 아니라 애플리케이션 로직이다.

### 코드

- `domain/kindergarten/` 신규 패키지 — domain/application/adapter 헥사고날 계층
- Flyway migration (`src/main/resources/db/migration/V3__*.sql` — V1·V2는 auth 도메인이 이미 사용 중)
- Flyway migration (`V4__kindergartens_road_address_only.sql`) — 리뷰 중 결정된 지번 주소 폐기·상세 주소 신설 반영(V3는 이미 공유된 마이그레이션이라 직접 수정하지 않고 V4로 추가, `database-change.md`)

### 문서

- `docs/adr/0011-<slug>.md` (신규, append-only) — "방향 논의 및 결정 사항"의 확정 사항을 결정 기록으로 남긴다
- `docs/domains/kindergarten.md` (신규) — 도메인 경계, 식별자 정책(`placeId`), 이관 상태 등 장기 기억

## 작업 제외 범위

| 항목 | 근거 |
|---|---|
| `map-view`, `map-view/aggregation`, `{id}/near`, `autocomplete`, `filters/result` | 지도/좌표 기반 동적 조회. 별도 하위 작업으로 분리한다. DB를 정답으로 미리 계산하고 Redis를 캐시로만 쓰는 방향을 검토했으나, 구체 설계(줌 레벨별 버킷 등)는 그 하위 작업에서 확정 |
| `GET /kindergarten`, `/aggregations`, `/filters`, `POST /load-csv` | `DROP` 확정 (ADR 0004 — map-view와 중복, load-csv는 인증 없는 위험 API) |
| `GET /{placeId}/blog-reviews` | `DEFER` (ADR 0005 리뷰 크롤링 마이그레이션 보류) |
| `POST /{id}/change-requests` | 조회가 아닌 쓰기. 대상 데이터(`kg_change_report`/`kg_change_evidence`)가 `DEFER` |
| 원장 권한/유치원 프로필 편집 | owner/authz 슬라이스 후속 작업. auth/User 기반(KD3-258)은 이미 확보됨 |
| 재크롤링 배치, 유치원 폐업 감지·처리 | 후속 작업. 크롤링에서 사라진 유치원을 폐업 처리할지는 결정하지 않고 미결 질문으로만 남긴다 |
| 실데이터 이관(레거시 운영 DB → 신규 DB) | ADR 0010 — 최후순위, 이번 범위 밖 |
| 롤백 안전성 설계 | 아래 "확정 사항" 참고 — 별도 설계 없이 리스크를 수용하기로 결정 |

## 방향 논의 및 결정 사항

### 확정 사항

- **인프라 분리**: 신규 서버·DB·Redis는 레거시와 완전히 분리한다. 레거시와 인프라를 공유하는 무중단 마이그레이션(ADR 0008 원안)은 하지 않는다.
- **API 버전 정책**: 기존 `v0`와 계약이 같은 API는 `v0`를 그대로 유지한다. 계약이 바뀌는 경우(RESTful 재설계, 응답 형식 변경 등) `v1`을 신규로 만들되, `v0`는 `v1` 위에 얹은 프록시(어댑터)로 유지해 프론트가 코드 변경 없이 계속 `v0`를 호출해도 동작하게 한다. 이번 티켓의 3개 엔드포인트(`main`/`basic`/`pricing`)는 계약 변경이 없어 전부 `v0` 단독으로 간다 — `main`의 향후 `v1` 재설계 여부는 별개 논의(아래 참고).
- **데이터 source of truth 전환**: 크롤링 JSON/Redis 대신 DB(MySQL)를 정답으로 삼는다. JSON을 파싱해 DB에 적재하고, 이후 DB가 source of truth다.
- **식별자**: 네이버 지도 `placeId` 기준. 원장이 자체 등록(네이버 미등재)하는 유치원은 레거시 `manual_` 접두사 관례를 계승해 별도 발급 ID를 부여한다.
- **폐업 상태값**: 레거시 DB 측엔 이미 `SchoolStatus`(`ACTIVE`/`CLOSED`) 개념이 있고, 이번 스키마의 `kindergartens.status` 컬럼이 그 대응이다. 다만 `info_new.json`(크롤링 원본)엔 폐업을 알려주는 필드가 없다 — 크롤링 시점에 존재가 확인된 유치원만 담기는 구조라, 시딩 시 전부 `ACTIVE`로 들어간다. `CLOSED`로 갱신하는 로직(재크롤링에서 사라짐 감지 등)은 후속 작업이다.
- **롤백 안전성**: 이번 컷오버는 별도 롤백 설계를 하지 않는다. 롤백이 필요할 상황을 낮게 보고 리스크를 수용하기로 결정했다(근거 상세 미기록 — 필요 시 보완).
- **KD3-335 흡수**: 컷오버 전략 결정은 원래 별도 티켓(KD3-335)이었으나, `003-migration.md` 2단계가 이 티켓 자체의 승인 조건으로 요구하는 내용과 동일해 KD3-413에 흡수했다. KD3-335는 삭제됨.
- **Jira 구조**: KD3-272를 에픽에서 작업(Task)으로 낮추고, KD3-273(폐기)을 대체해 KD3-413을 그 하위 작업으로 생성했다. `docs/domains/`, `docs/adr/` 등은 append-only/최신화 원칙을 그대로 따르므로 이 구조 변경과 무관하다.
- **`summary`(구 `main/{id}`) 재포함**: 위 "API" 참고 — `bookmarked`/`memoData`는 넣지 않고 나머지 필드는 구현한다.
- **신규 서버는 `v0`를 만들지 않는다**: 유치원 도메인에서 시작된 논의가 일반 정책으로 굳어져 [`ADR 0012`](../adr/0012-신규-서버-v0-미제공-원칙.md)로 분리했다. `docs/rules/api-migration.md`도 이 정책에 맞춰 갱신했다(§1, §2 전면 개정 — "기본값은 v0 단독" → "v0는 만들지 않는다"). 이미 머지된 auth 도메인 v0(login/refresh/logout/약관 동의)는 소급 적용 대상이 아니다.
- **주소는 도로명 주소만 저장**: 리뷰 중 지번 주소는 더 이상 저장하지 않고 도로명 주소만 저장하기로 결정. 상세 주소(`addressDetail`, nullable)를 신설. `V4__kindergartens_road_address_only.sql`로 반영(`kindergartens.address` 컬럼을 도로명 주소 값으로 교체, `address_detail` 컬럼 추가) — `info_new.json` 435건 전수 확인 결과 `road_address`가 전부 비어있지 않아 `address` NOT NULL을 유지했다.
- **코드 내 설명 주석 금지**: TODO성 주석을 제외하고 코드 사이 설명 주석(KDoc 포함)을 작성하지 않기로 결정. 유치원 도메인 전체 파일에서 기존 주석을 제거했다. 앞으로 이 코드베이스에 적용되는 일반 컨벤션이다.

### 미결 질문

- 유치원 폐업 감지·처리 정책(스키마의 `status` 컬럼을 언제·어떻게 `CLOSED`로 갱신할지) — 재크롤링 배치를 만드는 후속 작업에서 결정
- `map-view`/`near`/`autocomplete`의 조회 아키텍처(DB 사전 계산 + Redis 캐시 여부, 줌 레벨 버킷 설계) — 해당 하위 작업에서 결정
- `epic/KD3-272-kindergarten-schema` 브랜치가 향후 KD3-272의 다른 하위 작업(예: `comparison` 도메인의 "비교하기" 관련 작업)도 같이 태울지 — 지금 정하지 않고 그 작업이 실제로 생길 때 판단
- ~~`v0`를 이 서버가 계속 host할지~~ — **해결됨.** 이 서버는 `v0`를 만들지 않기로 최종 결정(ADR 0012). `KindergartenController`(v0)와 v0 전용 응답 DTO를 삭제하고 v1 파일들에서 `V1` 접미사를 뗐다.

### 사용자 승인 기록

- 위 확정 사항은 대화를 통해 단계적으로 결정됐고, KD3-413 Jira 티켓 본문(2026-09-01)에 이미 반영돼 있다.
- 2026-09-02 "구현 진행" 지시로 3단계 착수 승인됨.

## 완료 확인 기준

- ArchUnit(`HexagonalArchitectureTest`, `main/{id}`의 pure-domain 규칙을 auth 전용에서 전체 도메인 공통으로 일반화) — **통과**
- ktlint(`ktlintCheck`) — **통과**
- 단위/통합 테스트(도메인 로직, 매퍼, 서비스) — **73개 전부 통과**, `KnockdogApplicationTests`(전체 Spring 컨텍스트 로드) 포함. 상세는 다음과 같다:
  - `KindergartenDistanceCalculatorTest`, `KindergartenOperatingStatusCalculatorTest` — TDD(RED 확인 후 구현)로 작성
  - `GetKindergartenServiceTest` — TDD로 작성(naverPlaceId 기준 조회로 설계 변경 시 RED 재확인 포함)
  - `KindergartenSeedConverterTest`, `KindergartenSummaryResponseTest` — 구현 후 작성(회귀 안전망 목적, 순수 TDD는 아님), 주소 스키마 변경(도로명 주소만 저장)에 맞춰 갱신
  - `KindergartenJsonSeederTest` — TDD로 신규 작성. 실제 크롤링 JSON의 미매핑 필드(`business_services` 등)로 `UnrecognizedPropertyException`이 나던 버그를 RED로 재현 후 `FAIL_ON_UNKNOWN_PROPERTIES=false`로 수정
- `docs/adr/0011-유치원-도메인-신규db-단발컷오버.md` 작성 — **완료**
- **`KEEP` API 로컬 응답 대조** (`003-migration.md` 4단계) — **미완료.** 로컬에서 `./gradlew bootRun --args='--spring.profiles.active=local'`을 실제로 띄워보려 했으나:
  1. `.env.local`을 통째로 `source`하면 빈 값(`DB_HOST=` 등)이 실제 OS 환경변수로 export되어 `application-local.yaml`의 `${VAR:default}` 기본값이 적용되지 않고 타입 바인딩이 깨진다(`spring.jpa.show-sql` 등) — `.env.local` 사용법 자체의 함정으로 보이며 이번 티켓과 무관.
  2. 그 문제를 피해 필요한 값만 export해도, 이 워크트리들이 공유하는 로컬 MySQL 컨테이너(`knockdog-mysql-local`)의 Flyway 이력이 다른 워크트리/브랜치가 남긴 상태와 어긋나 `V1` 체크섬 불일치로 마이그레이션이 실패한다(공용 컨테이너를 임의로 초기화하면 다른 세션에 영향을 줄 수 있어 시도하지 않았다).
  - 그 결과 실제 서버를 띄워 레거시와 응답을 대조하지 못했다. **사람이 별도로 수행해야 한다** — 전용 로컬 DB 인스턴스를 쓰거나 공용 컨테이너의 Flyway 이력을 정리한 뒤, `docs/domains/kindergarten.md` §2의 "알려진 계약 차이" 목록을 실제 응답과 대조한다.
- **시딩 데이터 검증**(`database-change.md` §4) — **미완료**, 위와 같은 이유로 실제 시딩 실행 자체를 못 했다. `KindergartenJsonSeeder`는 `kindergarten.seed.enabled=true`(로컬 프로필 기본값)일 때 기동 시 자동 실행되도록 구현은 돼 있다.
- Notion API 명세 갱신 — `v0` 3개는 계약을 바꾸지 않는 구현 이관이라 새 페이지가 필요 없다. `v1` 3개(`summary`/`detail`/`pricing`)는 신규 API라 `notion-api-spec-sync.md` 절차대로 페이지를 새로 만들어야 한다. 이 세션이 가진 Notion 연동(OAuth 기반 MCP)이 그 절차가 요구하는 `API_NOTION_KEY` 토큰 방식과 달라 시도하지 않았다 — **사람이 해야 한다**.

## 작업 후 확인 목록

`docs/rules/documentation.md` §1 기준 판정:

| 문서 | 판정 근거 | 상태 |
|---|---|---|
| `docs/adr/0011-유치원-도메인-신규db-단발컷오버.md` | 되돌리기 어려운 결정(source of truth 전환, 컷오버 방식, v0/v1 정책) 확정 | **갱신함** — 신규 작성 |
| `docs/domains/kindergarten.md` | 도메인 경계·식별자 정책·이관 상태·알려진 계약 차이 등 장기 기억 확정 | **갱신함** — 신규 작성 |
| `docs/inventory/database.md` | `tb_school*` 4행에 신규 크롤링 기반 스키마(`kindergartens` 등)로의 별도 구축 사실을 후속 확인에 남김. 판정/진척 자체는 안 바꿈(원장 오버라이드 테이블은 여전히 미착수) | **갱신함** |
| `docs/inventory/api.md` | `main/{id}`, `basic/{id}`, `{id}/pricing`의 이관 진척을 `완료`로 갱신, `v1` 3개 신규 행 추가, `work/` 링크·알려진 계약 차이 참조 추가. 5절 요약 카운트 갱신 | **갱신함** |
| Notion API 명세 | `v1` 3개는 신규 API라 `notion-api-spec-sync.md` 대상(API 추가) | **미착수 — 사람 몫**(위 "완료 확인 기준" 참고, 이 세션의 Notion 연동 방식이 안 맞음) |
| `docs/rules/api-migration.md` | "신규 서버는 v0를 만들지 않는다"가 이 문서의 기존 "기본값은 v0 단독" 원칙과 정면으로 충돌 | **갱신함** — §1(기본 원칙)·§2(전면 개정, "v1을 새로 만들 것인가"→"새 경로 이름")·§4(path 변경 기준 삭제)를 새 정책에 맞게 고쳤다 |
| `docs/adr/0012-신규-서버-v0-미제공-원칙.md` | 여러 도메인에 영향을 주는 결정 확정 | **신규 작성** — append-only, 0004의 v0 기본 유지 원칙을 대체하는 결정이라 맥락 절에서 0004를 언급 |
| `docs/workflows/003-migration.md` | "KEEP API 로컬 응답 대조" 절이 `api-migration.md` §2를 인용하던 게 개정 후 어긋남, "경로 대조"도 더 이상 의미 없어짐 | **갱신함** — 인용 절 번호 수정(§2→§1), 이 대조가 경로가 아니라 응답 내용만 본다는 점 명시 |
| `docs/inventory/api.md`의 다른 도메인 행(`v0+v1`로 표시된 미착수 행 다수 — auth email-verification, pet, user, business-registration, owner-verification 등) | 같은 "v0를 안 만든다" 원칙과 충돌하나 이번 티켓 범위 밖 | **미착수 — 후속 확인으로 남김.** 각 도메인 담당자가 실제 착수할 때 반영하는 게 맞다고 판단해 일괄 수정하지 않았다 |
