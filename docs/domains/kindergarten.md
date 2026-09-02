> 생성: 2026-09-02 · 최종 수정: 2026-09-02 16:55

# kindergarten 도메인 마이그레이션 지시서

- 설계 근거: [`0010`](../adr/0010-신규-db-인스턴스-스키마-재작성.md)(신규 DB 인스턴스), [`0011`](../adr/0011-유치원-도메인-신규db-단발컷오버.md)(source of truth 전환), [`0012`](../adr/0012-신규-서버-v0-미제공-원칙.md)(신규 서버는 `v0`를 만들지 않는다 — 일반 정책). 컷오버 논의 배경은 [`docs/work/KD3-413-kindergarten-static-lookup.md`](../work/KD3-413-kindergarten-static-lookup.md).
- 원본: `daeng_v1_back`의 `kindergardeninfo/` 패키지. 조회 API의 실제 source of truth는 MySQL이 아니라 **Redis**였다(크롤링 JSON을 적재한 캐시) — `tb_school*`은 원장 인증 후 프로필 오버라이드만 담당.
- **KD3-413에서 정적 조회(요약/상세/요금표) `v1` 3종 구현 완료.** 이 서버는 레거시 `v0`(`main`/`basic`/`pricing`)를 만들지 않는다 — 기존 `v0` 클라이언트는 레거시 서버가 계속 응답한다. 지도/좌표 기반 동적 조회(map-view, near, autocomplete, filters/result)는 미착수(후속 하위 작업).

## 0. 담당 데이터

| 테이블 | 비고 |
|---|---|
| `kindergartens` | 루트. `naver_place_id`가 **대외 식별자**다(내부 PK가 아니다 — `GET /api/v1/kindergartens/{id}/**`의 `{id}`가 받는 값). 원장 자체 등록(네이버 미등재) 유치원은 레거시 `manual_` 접두사 관례를 계승한다. `status`(ACTIVE/CLOSED) 컬럼은 있으나 크롤링 시딩은 항상 ACTIVE로 채운다 — 폐업 감지는 후속 재크롤링 작업 |
| `kindergarten_categories` | 1:N, 업종(KINDERGARTEN/HOTEL 등). `summary`에서만 쓰인다 |
| `kindergarten_business_hours` | 1:N, 프로필별(DEFAULT/KINDERGARTEN/HOTEL 등 `name`으로 구분 — 실제로 나뉘어 관리되는 사례가 있어 유지). 브레이크타임·공휴일 휴무 컬럼은 없다 — 크롤링 원본에 그 개념 자체가 없다(§2 참고) |
| `kindergarten_links` | 1:N, `code`(INSTAGRAM/BLOG/HOMEPAGE/YOUTUBE 등)는 크롤링 값을 그대로 담는 열린 문자열 |
| `kindergarten_options` | 1:N, `option_group`(DOG_BREED/DOG_SERVICE/SAFETY_FACILITY/VISITOR_AMENITY, 우리가 정한 4분류 — enum)+`option_code`(크롤링 값, 열린 문자열). **`display_order` 컬럼 없음** — 그룹 내 노출 순서가 유치원마다 달라지는 데이터가 아니라고 판단해 뺐다. 순서가 필요해지면 애플리케이션 레벨 고정 매핑으로 처리한다 |
| `kindergarten_price_images` | 1:N, 크롤링 원본은 `info_new.json`의 `menu_image_s3_keys` |
| `kindergarten_menus` | 1:N, 크롤링 원본은 `price_and_product.json`(kindergarten_id로 join) |
| ~~`kindergarten_gallery_images`~~ | **스키마에 없음** — 레거시 `main/{id}`의 `banner`는 `[thumbnail_s3_key] + menu_image_s3_keys`를 이어붙인 것이라(레거시 `KindergartenMapper` 확인), 별도 갤러리 테이블이 필요 없다 |
| ~~`kindergarten_avg_prices`~~ | **스키마에 없음** — `avg_price_per_time.json`은 `main`/`basic`/`pricing` 어디에도 안 쓰이고 필터용 태그 생성에만 쓰이는 것으로 보인다(확신도 중간). map-view 등 후속 작업에서 실제로 필요해지면 추가한다 |

시딩 대상 JSON은 `info_new.json` + `price_and_product.json` 둘뿐이다. `product_pricing.json`은 `comparison` 도메인 소관이고, 자치구별 원본 파일(`dobonggu.json` 등 25개)·`info_and_review.json`은 어떤 코드에서도 읽지 않는 것으로 보여(미검증) 시딩 대상에서 제외했다.

## 1. 마이그레이션 대상 엔드포인트

이 서버는 `v0`를 만들지 않는다([`0012`](../adr/0012-신규-서버-v0-미제공-원칙.md)) — 아래 표의 "레거시 v0"는 카탈로그 목적으로만 남긴다. 실제 이 서버가 제공하는 경로는 `v1`뿐이다.

| 레거시 `v0` | 판정 | 이 서버의 경로 | 상태 |
|---|---|---|---|
| `GET /api/v0/kindergarten/main/{id}` | `KEEP` | `GET /api/v1/kindergartens/{id}/summary` | **구현 완료** — `address`/`roadAddress` 분리, `operationStatus`에 `HOLIDAY` 추가(레거시 버그 수정). `bookmarked`/`memoData` 필드 자체가 없음(도메인 생기면 추가) |
| `GET /api/v0/kindergarten/basic/{id}` | `KEEP` | `GET /api/v1/kindergartens/{id}/detail` | **구현 완료** — 실체 없는 `breakTime` 필드 제거(레거시 버그 수정) |
| `GET /api/v0/kindergarten/{id}/pricing` | `KEEP` | `GET /api/v1/kindergartens/{id}/pricing` | **구현 완료** — 응답 모양은 레거시와 동일(고칠 버그 없었음) |
| `GET /api/v0/kindergarten/map-view`, `/map-view/aggregation`, `/{id}/near`, `/autocomplete` | `KEEP` | (미정) | 미착수(후속) |
| `GET /api/v0/kindergarten/filters/result` | `REDESIGN` | `GET /v1/kindergartens/count`(가칭) | 미착수 — 이름이 실제 동작(개수 반환)과 달라 재설계 대상(ADR 0004) |
| `GET /api/v0/kindergarten`, `/aggregations`, `/filters`, `POST /load-csv` | `DROP` | 없음 | 해당없음 (ADR 0004) |
| `GET /api/v0/kindergarten/{placeId}/blog-reviews` | `DEFER` | 없음 | 해당없음 (ADR 0005) |
| `POST /api/v0/kindergarten/{id}/change-requests` | 미판정 | (미정) | 미착수 — 조회가 아닌 쓰기, 대상 데이터(`kg_change_report`) 자체가 `DEFER` |

기존 `v0` 3개(`main`/`basic`/`pricing`)를 호출하는 클라이언트는 레거시 서버가 계속 응답한다 — 이 서버 코드에는 없다.

## 2. 레거시에서 발견해 `v1`에서 고친 버그 (로컬 응답 대조 시 참고)

레거시 코드를 추적하며 발견한 버그들이다. 응답 **내용**이 레거시와 기능적으로 같은지 확인할 때(`003-migration.md` §4 "KEEP API 로컬 응답 대조") 아래 차이는 의도된 수정이지 대조 실패가 아니다.

| 항목 | 레거시 동작 | `v1`에서 고친 점 |
|---|---|---|
| `roadAddress` 필드 (summary, detail 둘 다) | 이름과 달리 실제로는 `address`(지번 주소) 값이 들어간다 — 레거시 `KindergartenMapper`가 `dto.getAddress()`를 그대로 넣는 버그 | `address`/`roadAddress`를 분리해 각자 실제 값을 담는다 |
| `operationStatus` (summary) | `businessStatus.title`이 "영업중"일 때만 `OPEN`, 그 외(휴무 포함)는 전부 `CLOSED` — `OperationStatusParser.parseOperationInfo`가 HOLIDAY를 구분하지 않는 버그 | 휴무일엔 `HOLIDAY`를 명시적으로 구분한다 |
| `detail`의 `operationTimes[].weekday/weekend[].breakTime` | 실제로는 마감 시각이 들어간다 — "open~close" 문자열을 "~" 기준으로 쪼개 두 번째 조각을 `breakTime`으로 넣는 버그(크롤링 데이터엔 브레이크타임 개념 자체가 없다) | `breakTime` 필드를 아예 뺐다 |

**아직 안 고친(그대로 남은) 근사치 — 후속 확인 필요:**

| 항목 | 내용 |
|---|---|
| `lastUpdatedAt`류 | 크롤링 원본에 타임스탬프 필드가 없어 고정 문구("정보 없음")를 반환한다. 원장 편집 이력이 생기면 실제 값으로 교체 필요(이번 범위 밖) |
| `pricing`의 `count` 필드 | 레거시가 내부 `Menu` 객체의 `totalTime`/`count` 조합으로 만드는데 정확한 원본 필드 대응을 확인하지 못해, 크롤링 원본 `unit_str`을 그대로 쓴다(근사치) |
| not-found 시 status code | 레거시 `KindergartenNotFoundException`은 전용 핸들러가 없어 500으로 응답한다(`GlobalControllerAdvice`의 catch-all). 신규 서버는 `CommonErrorCode.RESOURCE_NOT_FOUND`(404)로 교정했다 — 프론트가 이 케이스를 분기하는지 확인 필요 |
| `serviceTags` (summary) | 4개 옵션 그룹(견종/서비스/안전시설/편의시설) 코드를 합친 목록이다. 레거시의 `OPEN_NOW`/가격정책 파생 태그(영업중 여부, 횟수권/정기권/멤버십 보유 여부)는 포함하지 않는다 |

## 3. 검증 상태

- 단위/통합 테스트(도메인 로직, 매퍼, 서비스, ArchUnit), ktlint: 통과.
- **로컬 HTTP 응답 대조(레거시 `v0` vs 신규 `v1`)는 완료하지 못함** — 로컬 실행 환경 이슈(공용 MySQL 컨테이너의 Flyway 체크섬이 다른 워크트리 상태와 어긋남, JWT 시크릿 등 인증 도메인 설정 필요)로 실제 서버 기동 검증이 막혔다. 사람이 별도로 수행해야 한다(`003-migration.md` §4 "KEEP API 로컬 응답 대조" 절차 — 경로가 아니라 응답 내용만 대조).
- 시딩 데이터 검증(JSON 원본 대비 DB row count, 샘플 비교)도 같은 이유로 미완료.
- `v1` 3개 API의 Notion 명세 페이지 등록은 미착수(사람 몫).

## 4. 후속 결정 사항

- `epic/KD3-272-kindergarten-schema` 브랜치를 이 도메인의 다음 하위 작업(map-view 등)도 계속 태울지는 그 작업 착수 시점에 정한다.
- 유치원 폐업 감지·처리, `kindergarten_avg_prices` 필요 여부는 map-view/재크롤링 후속 작업에서 재확인한다.
- Notion API 명세에 `v1` 3개 페이지 신규 등록(사람 몫, §3 참고).
- `map-view`/`filters/result` 등 후속 작업도 착수 시 이 서버엔 `v1`(또는 그에 준하는 신규 버전)만 만든다([`0012`](../adr/0012-신규-서버-v0-미제공-원칙.md)) — 매번 다시 논의할 필요 없음.
