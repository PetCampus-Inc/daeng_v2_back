> 생성: 2026-09-09 16:30 · 최종 수정: 2026-09-10 15:00

# KD3-469 유치원 비교 조회 API 개발

| 항목 | 값 |
|---|---|
| Jira | `KD3-469` |
| 브랜치 | `feat/KD3-469-kindergarten-comparison` |
| 상위 에픽 | `KD3-272` (유치원 도메인 마이그레이션) |

## 현재 제어점

- 활성 workflow: `003-migration`
- 현재 공통 단계: `5` (독립 리뷰·PR·문서 동기화)
- 다음 결정 또는 전환 조건: 독립 리뷰 반영 → PR → epic 머지

## 작업 목표

레거시 `GET /api/v0/kindergarten/comparisons`(인벤토리 판정 `KEEP`)를 신규 서버 `GET /api/v1/kindergartens/comparisons`로 이관한다. ADR 0012에 따라 `v0` 경로는 만들지 않고 `v1`만 제공하며, 응답 **내용**이 레거시와 기능적으로 같아야 한다(경로는 다름).

- 유치원 정확히 2곳을 받아 나란히 비교할 데이터를 배열로 반환한다(§확정 사항).
- 응답 필드: `id`, `name`, `thumbnailS3Key`, `categories`, `pricing`, `service`, `distance`, `operatingSchedule` (레거시 `ComparisonResponse`와 동일).
- 이동시간(`distance[].transitTimes`)과 비교 히스토리는 이 티켓 범위 밖(각각 KD3-499, KD3-496).

### 배경

- 레거시 `comparison` 도메인은 조회 API 1개 + 히스토리 API 2개로 구성된다. 히스토리는 KD3-496으로 분리했고, 여기서는 조회 API만 다룬다.
- 레거시 조회 API는 로그인 없이 호출 가능하며(`userId` optional), 로그인 시 비교 히스토리를 **GET의 부수효과로** 저장했다. 이 저장은 KD3-496 소관이라 이 티켓에서는 하지 않는다.
- 레거시는 유치원 데이터를 Redis(`kindergarten:{id}`)에서, 가격 집계를 Redis(`kindergarten:{id}:pricing`, `product_pricing.json` 적재)에서 읽었다. v2는 유치원 데이터를 RDB로 이관 완료(KD3-413), 가격 집계는 미이관.

### 요금 집계 — 레거시 재현이 아니라 재계산

레거시 `ComparisonResponse.Pricing`은 크롤러가 미리 만든 `product_pricing.json`에서 왔다. 이 파일은 v2가 이미 시딩한 `price_and_product.json`(→ `kindergarten_menus`)보다 **오래된 크롤**이다 — 같은 `naver_place_id`인데 상품 목록·가격이 서로 다르다(확인: id `1253111667`의 `product_pricing.json`에는 "호텔링(24H)" 상품이 있으나 `price_and_product.json`에는 없음). 따라서 `product_pricing.json`을 시딩해 "레거시 값 재현"을 하면 낡은 데이터를 내보내는 셈이다.

대신 `kindergarten_menus`에서 재계산한다. `kindergarten_menus`는 행마다 `productType`(요금정책: `COUNT_TICKET`/`MONTHLY_TICKET`/`MEMBERSHIP`), `serviceType`(`DAYCARE`/`NIGHT_CARE`/`TRAINING`/`MEMBERSHIP`), `price`, `hourlyPrice`, `productName`을 갖는다.

레거시 평균값의 출처를 특정할 수 없다. `avg_price_per_time.json`도 `product_pricing.json`과 마찬가지로 `price_and_product.json`보다 낡은 크롤이다 — 394개 `(kindergarten × productType × serviceType)` 그룹 중 322개(82%)만 `round(mean(hourly_price))`와 일치하고, 나머지는 레거시 값이 더 크다(일관된 배수도 아님). 크롤러의 집계 공식은 이 저장소들에 없다.

따라서 레거시 값을 재현하지 않고, **우리가 정한 의미로 `kindergarten_menus`에서 계산한다.** 프론트 툴팁("두 곳의 이용료를 1시간 기준으로 맞춰 비교")·라벨("횟수권 (1h)" / "정기권 (1h)")이 요구하는 의미는 "요금정책별 시간당 평균가"이고, 그건 `mean(hourlyPrice)`다. 값이 레거시와 다른 것은 레거시 소스가 stale이라 의도된 차이다. 프론트는 이 값을 절대값 표시(포맷팅)와 두 유치원 비교에만 쓰므로 영향 없다.

| 응답 필드 | 계산 (`kindergarten_menus` 기준) |
|---|---|
| `pricing.countHourlyAvg` | `round(mean(hourlyPrice))`, `productType == COUNT_TICKET` 전체. hourlyPrice null 행 제외. 대상 없으면 `0` |
| `pricing.monthlyHourlyAvg` | 〃 `productType == MONTHLY_TICKET` |
| `pricing.products[svc].min` / `.max` | `serviceType == svc`에서 `price` 최소/최대인 메뉴 1건 → `{name: productName, price}`. `price` null 행 제외 |
| `pricing.products[svc].countTicketAvg` | `round(mean(hourlyPrice))`, `productType == COUNT_TICKET` ∧ `serviceType == svc`. 대상 없으면 `0` |
| `pricing.products[svc].monthlyHourlyAvg` | 〃 `productType == MONTHLY_TICKET` ∧ `serviceType == svc` |
| `pricing` 전체 | `menus`가 비면 `null` (프론트가 "가격 정보가 없어 비교가 어려워요" 처리) |

`products[]`는 `serviceType` 그룹별로 하나씩. 프론트는 `productType`(= 우리 `serviceType`)이 `DAYCARE`/`NIGHT_CARE`/`TRAINING`/`MEMBERSHIP` 중 하나라고 가정한다.

### 거리 — 직선거리만, 기준점은 레거시대로

레거시 `getUserAddresses` 우선순위를 그대로 따른다.

1. `lat`+`lng` 쿼리 있음 → 합성 기준점 1개, `referencePoint = OTHER`("공유된 위치")
2. 없고 로그인함 → 유저의 저장 주소 **전부**, 각 `referencePoint`는 주소 `type`(`HOME`/`OTHER`)
3. 둘 다 없음 → `distance: []`

각 (유치원 × 기준점)마다 `distance[]` 항목 1개: `{referencePoint, distance: "9.6km", transitTimes: []}`.

- `distance` 문자열 = `KindergartenDistanceCalculator.calculateKm` → `"%.1fkm"` (레거시와 같은 Haversine).
- `transitTimes`는 **빈 배열**. 채우는 것은 KD3-499.
- 유저 저장 주소는 auth 도메인 소관이다. comparison(=kindergarten 기능)이 auth의 `User.addresses`를 읽어야 하므로 아웃바운드 포트 하나를 새로 둔다(§방향 논의).

### 그 외 필드

| 필드 | 방법 |
|---|---|
| `categories` | `kindergarten.categories.map { it.value }` |
| `service` | `KindergartenServiceTags.allOf(kindergarten)` — 견종/서비스/안전시설/편의시설 4개 옵션그룹 코드 목록. 프론트 `DogServiceSection`이 대조하는 `TOTAL_SERVICE_MAP` 키와 1:1이다. 레거시 `ServiceTag` enum의 파생 태그(`OPEN_NOW` 등)는 프론트 비교 맵에 키가 없어 무시되므로 쓰지 않는다. v1 `summary`와 자동으로 일치 |
| `operatingSchedule` | `businessHours` 프로필 1개(`name == "DEFAULT"` 우선, 없으면 첫 번째) → 아래 구조. `weekday`/`weekend`는 `KindergartenDetailResponse.BusinessHours`의 `TimeRange`와 같은 모양 |

```text
operatingSchedule: {
  weekday: { open: LocalTime, close: LocalTime } | null,   // "09:00" / "20:00"
  weekend: { open: LocalTime, close: LocalTime } | null,
  closedDays: ["MONDAY", ...]                               // offday 이름
}
```

레거시는 `weekdayHours`/`weekendHours`를 `"09:00~20:00"` 문자열로 줬다. v1에서 `{open, close}` 구조로 개선한다(v2 `detail`과 일관, KD3-495로 `LocalTime`이 `"09:00"`으로 직렬화). 프론트 `createOperatingScheduleSlide`가 문자열을 직접 셀에 뿌리므로 **프론트 수정 필요**(§작업 제외 범위, 프론트 협의).

## 작업 범위

1. **요금 집계 계산기** — `kindergarten/domain/KindergartenPricingComparisonCalculator`(순수 함수, `KindergartenDistanceCalculator`·`KindergartenOperatingStatusCalculator`와 같은 결). 입력 `List<KindergartenMenu>`, 출력은 응답 DTO가 아니라 도메인 값(예: `KindergartenPricingComparison`). 위 표대로 계산.
2. **거리 기준점 조회 포트** — `kindergarten/application/port/output/LoadComparisonAddressesPort`. 어댑터(`kindergarten/adapter/outbound/user/ComparisonAddressAdapter`)가 auth `LoadUserPort.findByCode(UserCode)`를 호출해 `User.addresses` → `ComparisonReferencePoint`(kindergarten 도메인 값, `HOME`/`OTHER` + lat/lng)로 매핑. `@AuthenticationPrincipal`이 UserCode 문자열이라 `findByCode`를 쓴다. 유저 없으면 빈 목록.
3. **유치원 batch 로드** — `LoadKindergartenPort.findByNaverPlaceIds(ids: List<String>): List<Kindergarten>` + 어댑터 구현(`findAllByNaverPlaceIdIn` 후 기존 `assemble` 재사용). 요청 수와 로드 수가 다르면 서비스가 판단.
4. **use case / service** — `CompareKindergartensUseCase`(입력 포트) + `CompareKindergartensService`. ids 검증(정확히 2개·중복), 404 판정, 기준점 결정, 유치원별 비교 데이터 조립.
5. **컨트롤러 / 응답 DTO** — `KindergartenComparisonController`(`GET /api/v1/kindergartens/comparisons`), `KindergartenComparisonResponse`. `@AuthenticationPrincipal` nullable.
6. **에러 코드** — kindergarten 전용 에러 코드에 `COMPARISON_TARGET_COUNT`(400, "비교 유치원은 2곳이어야 합니다"), `COMPARISON_TARGET_DUPLICATED`(400) 추가. not-found는 기존 `RESOURCE_NOT_FOUND`(404).
7. **SecurityConfig** — `/api/v1/kindergartens/comparisons`를 `PUBLIC_ENDPOINTS`에 추가(레거시가 비로그인 허용).
8. **문서** — `docs/inventory/api.md` comparison 행(`GET /comparisons`) 이관 진척 갱신, `docs/domains/kindergarten.md`에 비교 API 절 추가.

## 작업 제외 범위

- `distance[].transitTimes` (도보·자동차·대중교통 소요시간) — KD3-499. TMAP/네이버 어댑터, Redis 캐시, API 키.
- 비교 히스토리 저장·조회·삭제 — KD3-496. 로그인 시 GET 부수효과 저장도 여기서 안 한다.
- `product_pricing.json` / `avg_price_per_time.json` 시딩 — 재계산으로 대체하므로 안 한다.
- 프론트(`daeng_v2_front`) 코드 수정 — 이 티켓에서 하지 않는다. 다만 아래는 **프론트 협의·후속 대응 필요**로 남긴다:
  - `v0` → `v1` 엔드포인트 전환(`kindergarten/comparisons` → `kindergarten/comparisons`, prefix 변경).
  - `operatingSchedule`가 `weekdayHours: string`에서 `weekday: {open, close}` 구조로 바뀜 — `createOperatingScheduleSlide` 수정 필요.
  - `transitTimes`가 빈 배열이라 거리 비교 섹션이 "-"로 나옴 — KD3-499까지의 알려진 저하.
- 유저 주소 조회 전용 공개 API — comparison 내부 포트로만 읽고, 별도 엔드포인트는 만들지 않는다.

## 방향 논의 및 결정 사항

### 확정 사항

- 요금 집계는 `kindergarten_menus`에서 재계산(레거시 `product_pricing.json` 시딩 안 함). 근거·검증은 §배경.
- 패키지 배치: 별도 `comparison` 도메인이 아니라 `domain/kindergarten`의 기능. 자체 영속성이 없고 `Kindergarten` 애그리거트를 통째로 읽는다. 히스토리(KD3-496)의 배치는 그때 결정.
- 거리 기준점: `lat`/`lng` 쿼리 + 로그인 저장 주소 둘 다 지원(레거시대로). 저장 주소는 `LoadComparisonAddressesPort` → auth `LoadUserPort.findByCode` 경유로 읽는다.
- 이동시간은 KD3-499로 분리. KD3-469는 `distance[]` 구조를 완성하고 `transitTimes`만 빈 배열.
- `service`는 `KindergartenServiceTags.allOf`(4개 옵션그룹 코드) 사용 — 프론트 `TOTAL_SERVICE_MAP` 키와 1:1.
- `operatingSchedule` 프로필은 `name == "DEFAULT"` 우선, 없으면 첫 번째.
- `operatingSchedule.weekday`/`weekend`는 `{open: LocalTime, close: LocalTime} | null` 구조(detail의 `TimeRange`와 동일). 레거시 문자열에서 v1 계약 개선.
- 비교 대상은 **정확히 2곳**. `ids.size != 2` → 400.
- `distance[]` 순서: `HOME` 먼저, 그다음 유저 주소 저장 순서.

### 미결 질문

1. **serviceTags 소스** — **해소(2026-09-10)**: `KindergartenServiceTags.allOf` 사용. 프론트 `DogServiceSection`이 대조하는 `TOTAL_SERVICE_MAP`(4개 옵션그룹 맵) 키와 정확히 일치. 레거시 `ServiceTag` enum 파생 태그는 프론트 비교 맵에 키가 없어 무시되므로 재현 불필요.
2. **`operatingSchedule` 프로필 선택** — **해소(2026-09-10)**: `name == "DEFAULT"` 우선, 없으면 첫 번째.
3. **`weekdayHours`/`weekendHours` 포맷** — **해소(2026-09-10)**: `{open, close}` 구조로 변경(detail `BusinessHours`와 동일). 프론트 수정 필요 항목으로 기록.
4. **비교 대상 수** — **해소(2026-09-10)**: 정확히 2곳. 레거시 "2 이상"에서 변경.
5. **`distance` 항목 순서** — **해소(2026-09-10)**: `HOME` 먼저, 그다음 저장 순서.

### 사용자 승인 기록

- 2026-09-10 — 작업 문서 검토, 미결 질문 5건 확정("미결 질문부터 처리하자" → 5건 답변 → "3번 businesshours 와 같은 모양으로 해서 진행"). 구현 진행 승인.
- 2026-09-10 — "중간 보고 없이 끝까지 쭉 작업해" — 구현·독립 리뷰·PR까지 논스톱 진행 위임.

### 독립 리뷰

컨텍스트 없는 리뷰어가 커밋 `ff71aaf..f0a2d1e`와 이 문서를 대조(2026-09-10). 빌드/컨벤션/커밋/아키텍처 이상 없음, 블로커 없음.

| 지적 | 처리 |
|---|---|
| KEEP 대조 표에 `operatingSchedule` 프로필 선택·`distance[]` 순서 변경 행 누락 | 표에 2행 추가 |
| `ComparisonAddressAdapter` 테스트 없음 | `ComparisonAddressAdapterTest` 추가(타입 매핑 양쪽, 유저 없음) |
| `>2 ids` → 400 테스트 없음 | `CompareKindergartensServiceTest`에 3곳 케이스 추가 |
| `"%.1fkm".format()` 기본 로케일 | `String.format(Locale.KOREA, ...)`로 고정 (레거시도 기본 로케일이라 회귀는 아님) |
| 메뉴는 있으나 price·hourlyPrice 전부 null이면 `pricing`이 `{0,0,[]}` (null 아님) | `products` 비고 두 평균 모두 0이면 `null` 반환하도록 계산기 수정 + 테스트 |
| 계산기 `?: 0` 죽은 코드 | 제거(price를 Pair로 들고 다님) |
| kindergarten 어댑터가 auth의 **outbound** 포트(`LoadUserPort`)를 호출 — auth가 inbound 계약을 발행하는 게 더 깨끗 | 후속(아래). 지금은 read-only·소규모라 수용 |
| `roundToInt`는 half-up, 검증 문서의 82% 분석은 Python `round`(half-even) | 레거시 재현이 목적이 아니라 영향 없음. 문서에 표기 |

### 후속 작업

- **응답 DTO 시간 타입 제한 ArchUnit 규칙** — (KD3-495에서 넘어옴) `adapter/inbound/web` 응답 DTO가 `Instant`/`OffsetDateTime` 등을 쓰지 못하게 강제.
- **cross-domain `application` 접근 규칙** — auth가 유저 조회용 inbound 계약(use case)을 발행하고, kindergarten이 그걸 쓰도록 정리. ArchUnit으로 도메인 간 `application` 직접 참조를 막는 것도 검토. 별도 티켓.

## 완료 확인 기준

### 테스트 (2026-09-10, `./gradlew clean test ktlintCheck` — 총 154개, 실패 0, ArchUnit 통과)

- `KindergartenPricingComparisonCalculatorTest` (7) — min/max, 정책별·서비스별 `round(mean(hourlyPrice))`, hourlyPrice null 행 제외, 대상 없으면 0, price·hourlyPrice 전부 null → null, price 전부 없는 서비스종류 제외, 빈 메뉴 → null.
- `CompareKindergartensServiceTest` (8) — ids 2개 아님/3곳 → `COMPARISON_TARGET_COUNT`, 중복 → `COMPARISON_TARGET_DUPLICATED`, 없는 id → `RESOURCE_NOT_FOUND`, 결과 순서 = 요청 순서, lat/lng 기준점 = OTHER 1개, 로그인 주소 기준점 HOME 먼저, 비로그인·위치없음 → 빈 목록.
- `KindergartenComparisonResponseTest` (6) — pricing 조립, `operatingSchedule` DEFAULT 프로필 우선, 영업시간 빈 값 → null, distance 기준점별 직선거리 값(`"7.6km"`/`"8.8km"`) + `transitTimes` 빈 배열, 좌표 없는 유치원 → distance 빈 배열.
- `KindergartenComparisonEndpointTest` (6, `@SpringBootTest`+MockMvc) — 비로그인 200, `operatingSchedule.weekday.open`이 `"09:00"` JSON으로, ids 2개 아님 400 `COMPARISON_TARGET_COUNT`, 없는 유치원 404 `RESOURCE_NOT_FOUND`, lat/lng → OTHER 1개, 로그인 → 저장 주소(HOME) 기준점.
- `ComparisonAddressAdapterTest` (2) — `AddressType` 매핑 양쪽, 유저 없음 → 빈 목록.

### KEEP API 로컬 응답 대조 (`003-migration.md` §4)

레거시 서버(Redis 의존)를 로컬에서 띄우지 못해 **실행 대조는 못 했다**. 대신 레거시 `ComparisonResponse.java` / `ComparisonService.java` 소스와 v1 코드를 필드 단위로 대조했다.

| 필드 | 레거시 | v1 | 차이와 근거 |
|---|---|---|---|
| `id`/`name`/`thumbnailS3Key`/`categories` | KindergartenDto | Kindergarten(RDB) | 동일(크롤 원본) |
| `pricing.countHourlyAvg`/`monthlyHourlyAvg` | `product_pricing.json`(stale) | `round(mean(hourlyPrice))` | **값 다름 — 의도.** 레거시 소스가 stale(§배경) |
| `pricing` null | `:pricing` Redis 키 없을 때 | `menus` 비었을 때 | 트리거 조건 다름, 결과(null) 동일 |
| `pricing.products[].productType` → **`serviceType`** | 필드명 `productType` | 필드명 `serviceType` | **필드명 변경 — code-style §3.** 프론트 수정 필요 |
| `pricing.products[].min`/`max` | stale json | `menus`의 `price` MIN/MAX | 구조 동일(`{name, price}`), 값 다를 수 있음 |
| `pricing.products[].countTicketAvg` | stale json | `round(mean(hourlyPrice))` COUNT_TICKET∩svc | 값 다름 — 의도 |
| `service` | `ServiceTag` enum(영업중·가격정책 태그 포함) | `KindergartenServiceTags.allOf`(4개 옵션그룹) | **목록 다름 — 의도.** 프론트 `TOTAL_SERVICE_MAP`과 1:1(§미결 질문 1) |
| `distance[].referencePoint` | `HOME`/`WORK`/`OTHER` | `HOME`/`OTHER` | v2엔 `WORK` 없음(auth `AddressType`, KD3-372) |
| `distance[].distance` | `"%.1fkm"` Haversine | `"%.1fkm"` Haversine(동일 공식) | 동일 |
| `distance[].transitTimes` | `[{type, time: "2시간 49분"}]` | **`[]`** | **비어있음 — KD3-499로 분리** |
| `operatingSchedule` | `{closedDays, weekdayHours: "09:00~20:00", weekendHours}` | `{weekday: {open,close}, weekend: {open,close}, closedDays}` | **구조 변경 — 의도(§미결 질문 3).** `detail`과 일관. 프론트 수정 필요 |
| `operatingSchedule` 프로필 선택 | `businessHours.get(0)` (항상 첫 번째) | `name == "DEFAULT"` 우선, 없으면 첫 번째 | 의도(§미결 질문 2). v2는 `name`으로 구분된 여러 프로필을 가질 수 있음 |
| `distance[]` 순서 | 유저 저장 주소 순서 그대로 | `HOME` 먼저, 그다음 저장 순서 | 의도(§미결 질문 5). 프론트는 `referencePoint`로 찾아 써서 순서 무관 |
| not-found | 500(전용 핸들러 없음) | 404 `RESOURCE_NOT_FOUND` | 교정(summary/detail/pricing과 동일) |
| ids<2 / 중복 | `COMPARISON-400-1` / `COMPARISON-400-2` | `COMPARISON_TARGET_COUNT` / `COMPARISON_TARGET_DUPLICATED` | code 문자열 다름 — 프론트는 comparison 에러 코드로 분기 안 함(`shared/api/model/constant/apiErrorCode.ts`엔 login/token 코드만). 안전 |
| ids>2 | 허용 | 400 | 정확히 2(§미결 질문 4) |
| `Response` 래퍼 | `{data, status, code, message, responseTime}` | `{status, code, message, data}` | `responseTime` 없음 — 프론트 `select`가 `.data`만 읽음(KD3-258 선례) |

**프론트 협의 필요**: `productType`→`serviceType` 필드명, `operatingSchedule` 구조, `v0`→`v1` 경로. 사람이 프론트 저장소에서 전환 작업.

## 작업 후 확인 목록

- `docs/inventory/api.md` — `GET /api/v0/kindergarten/comparisons` 행 이관 진척 `미착수` → `진행중`/`완료`, 후속 확인에 이 문서 링크.
- `docs/domains/kindergarten.md` — 비교 조회 API 절 추가(제공 API 목록, 요금 재계산 근거, KD3-499/496 분리).
- `docs/inventory/integrations.md` — TMAP·네이버 행은 KD3-499에서 갱신(여기서는 손대지 않음).
- `docs/conventions/*` — 해당 없음.
