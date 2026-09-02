> 생성: 2026-09-01 21:05 · 최종 수정: 2026-09-02 16:10

# KD3-413 — 유치원 도메인 스키마 이관 및 정적 조회 기능

| 항목 | 값 |
|---|---|
| Jira | `KD3-413` |
| 브랜치 | `feat/KD3-413-kindergarten-static-lookup` |
| 상위 에픽 | `KD3-272`(Task) — git 브랜치는 `epic/KD3-272-kindergarten-schema` |

## 현재 제어점

- 활성 workflow: `003-migration`
- 현재 공통 단계: `4`(검증) — 구현은 끝났고, 아래 "완료 확인 기준"의 로컬 응답 대조·시딩 데이터 검증 2개가 환경 문제로 미완료다.
- 다음 결정 또는 전환 조건: 사람이 로컬 환경(공용 MySQL 컨테이너의 Flyway 이력, JWT 시크릿 등)을 정리해 나머지 검증을 마치거나, 이 상태로 5단계(리뷰·PR)로 넘어갈지 결정한다.

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
kindergartens                    루트: naver_place_id, name, address, road_address, lat/lng, phone_number, thumbnail_s3_key,
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

`v0`(레거시 계약 보존, 그대로 유지)와 `v1`(재설계, 신규 추가) 둘 다 이 서버에 둔다:

| Method | Path | 비고 |
|---|---|---|
| GET | `/api/v0/kindergarten/main/{id}` | 레거시 계약 그대로(버그 포함) |
| GET | `/api/v0/kindergarten/basic/{id}` | 레거시 계약 그대로(버그 포함) |
| GET | `/api/v0/kindergarten/{id}/pricing` | 레거시 계약 그대로 |
| GET | `/api/v1/kindergartens/{id}/summary` | 신규 — `address`/`roadAddress` 분리, `operationStatus`에 `HOLIDAY` 추가 |
| GET | `/api/v1/kindergartens/{id}/detail` | 신규 — 실체 없는 `breakTime` 필드 제거 |
| GET | `/api/v1/kindergartens/{id}/pricing` | 신규 — `v0`와 응답 모양 동일(고칠 버그 없음), 경로만 `v1` |

**`v1` 전면 전환 결정**: 처음엔 "이름이 실제 동작과 다를 때만 `v1`"(ADR 0011 원안)이었으나, 구현 중 "이번 기회에 3개 전부 `v1`으로 전환한다"로 바뀌었다. `v0`는 삭제하지 않고 남겨둔다 — **이 서버가 `v0`를 계속 host할지, 레거시로만 라우팅할지는 프론트 개발자 확인 후 결정**(미결, 아래 참고).

**`main/{id}`·`v1 summary` 재포함 결정**: 처음엔 `bookmarked`/`memoData`가 `bookmark`/`memo` 도메인에 의존해 범위에서 뺐으나(두 도메인 다 이 저장소에 없음, `memo`는 인벤토리에서도 `REDESIGN`/미착수), `v0`는 이 두 필드만 스텁(`bookmarked=false`, `memoData=null` 고정)으로 두고 나머지 필드는 구현했다. `v1 summary`는 애초에 이 두 필드를 넣지 않는다 — 신규 계약이라 스텁을 먼저 노출할 필요가 없고, 도메인이 생기면 필드를 추가하는 쪽이 더 자연스럽다. `bookmark`/`memo` 도메인이 생기기 전까지는 실제 데이터가 있어도(레거시 쪽) 신규 서버 응답엔 반영되지 않는다 — 신규 DB가 레거시와 분리돼 있어서다.

**거리 계산(`dist`)**: 레거시는 저장된 값이 아니라 요청마다 계산한다(`KindergartenQueryService.calculateDistance`) — 구면 삼각법 기반 근사 공식(위경도 차이 → `acos` → 60 * 1.1515로 해리를 마일로, `* 1.609344`로 km 변환)이고 `lat`/`lng` 쿼리 파라미터가 필수다. 정밀한 Haversine은 아니지만 오차가 크지 않은 근사식이라 그대로 이식하면 된다. 스키마에 저장할 값이 아니라 애플리케이션 로직이다.

### 코드

- `domain/kindergarten/` 신규 패키지 — domain/application/adapter 헥사고날 계층
- Flyway migration (`src/main/resources/db/migration/V3__*.sql` — V1·V2는 auth 도메인이 이미 사용 중)

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
- **`main/{id}` 재포함**: 위 "API" 참고 — `bookmarked`/`memoData`만 스텁으로 두고 이번 티켓에서 구현한다.
- **`main/{id}` 향후 네이밍**: `bookmark`/`memo` 도메인이 갖춰진 뒤에도 `v0` 이름(`main`)을 그대로 유지할지, `docs/rules/api-migration.md` "이름이 실제 동작과 다르다" 기준에 따라 `v1`으로 새 이름(상단 고정 헤더라는 의미가 드러나는 `/v1/kindergartens/{id}/summary`류)을 내고 `v0`는 프록시로 돌릴지는 여전히 유효한 논의이나 즉시 결정할 필요는 없다. `basic/{id}`는 "기본정보" 탭과 이름이 일치해 재설계 대상이 아니다. 이 정책은 `docs/domains/kindergarten.md` 신설 시 옮겨 담는다.

### 미결 질문

- 유치원 폐업 감지·처리 정책(스키마의 `status` 컬럼을 언제·어떻게 `CLOSED`로 갱신할지) — 재크롤링 배치를 만드는 후속 작업에서 결정
- `map-view`/`near`/`autocomplete`의 조회 아키텍처(DB 사전 계산 + Redis 캐시 여부, 줌 레벨 버킷 설계) — 해당 하위 작업에서 결정
- `epic/KD3-272-kindergarten-schema` 브랜치가 향후 KD3-272의 다른 하위 작업(예: `comparison` 도메인의 "비교하기" 관련 작업)도 같이 태울지 — 지금 정하지 않고 그 작업이 실제로 생길 때 판단
- **`v0`를 이 서버가 계속 host할지** — 지금은 `v0`(main/basic/pricing) 컨트롤러를 그대로 뒀다. 프론트 개발자가 확인해주면(트래픽을 LB에서 레거시로 바로 보낼지, 아니면 이 서버가 계속 응답할지) 그때 정리한다. 레거시로만 보내기로 하면 `KindergartenController`(`v0`)와 그 응답 DTO 3개를 삭제한다

### 사용자 승인 기록

- 위 확정 사항은 대화를 통해 단계적으로 결정됐고, KD3-413 Jira 티켓 본문(2026-09-01)에 이미 반영돼 있다.
- 2026-09-02 "구현 진행" 지시로 3단계 착수 승인됨.

## 완료 확인 기준

- ArchUnit(`HexagonalArchitectureTest`, `main/{id}`의 pure-domain 규칙을 auth 전용에서 전체 도메인 공통으로 일반화) — **통과**
- ktlint(`ktlintCheck`) — **통과**
- 단위/통합 테스트(도메인 로직, 매퍼, 서비스) — **72개 전부 통과**, `KnockdogApplicationTests`(전체 Spring 컨텍스트 로드) 포함. 상세는 다음과 같다:
  - `KindergartenDistanceCalculatorTest`, `KindergartenOperatingStatusCalculatorTest` — TDD(RED 확인 후 구현)로 작성
  - `GetKindergartenServiceTest` — TDD로 작성(naverPlaceId 기준 조회로 설계 변경 시 RED 재확인 포함)
  - `KindergartenSeedConverterTest`, `KindergartenSummaryV1ResponseTest` — 구현 후 작성(회귀 안전망 목적, 순수 TDD는 아님)
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
| `docs/rules/api-migration.md` | v0=v1프록시 패턴이 이미 서술된 정책과 합치하는지 확인 | **확인함, 변경 불필요** — 이번엔 `v0`/`v1` 내용 자체가 달라(버그 유지 vs 수정) 프록시 패턴을 적용하지 않기로 한 예외 사례이며, 그 근거는 ADR 0011에 남겼다(정책 문서 자체를 고칠 정도의 일반 규칙 변경은 아니라고 판단) |
