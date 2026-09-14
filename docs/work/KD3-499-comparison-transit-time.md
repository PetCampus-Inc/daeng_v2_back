> 생성: 2026-09-14 · 최종 수정: 2026-09-14

# KD3-499 유치원 비교 이동시간 연동 (TMAP/네이버 통합)

| 항목 | 값 |
|---|---|
| Jira | `KD3-499` |
| 브랜치 | `feat/KD3-499-transit-time` |
| 상위 에픽 | `KD3-272` (유치원 도메인 마이그레이션) |
| 선행 | `KD3-469`(비교 조회 API, 완료) |

## 현재 제어점

- 활성 workflow: `003-migration`
- 현재 공통 단계: `5` (독립 리뷰·PR·문서 동기화)
- 다음 결정 또는 전환 조건: 독립 리뷰 반영 → PR → epic 머지

## 작업 목표

KD3-469가 만든 `GET /api/v1/kindergartens/comparisons` 응답의 `distance[].transitTimes`(현재 항상 빈 배열, [`KindergartenComparisonResponse.kt:114`](../../src/main/kotlin/com/petcampus/knockdog/domain/kindergarten/adapter/inbound/web/KindergartenComparisonResponse.kt))를 실제 도보·자동차·대중교통 소요시간으로 채운다.

- 도보·자동차: TMAP 경로 API.
- 대중교통: TMAP 대중교통 API로 통합(레거시는 네이버 비공식 endpoint 사용 — §방향 논의 참고).
- 소요시간은 Redis에 캐싱한다(레거시 TTL 7일, 위경도 격자 해시 키).
- `transitTimes[].time`은 초 단위 `number`로 응답한다(레거시는 `"2시간 49분"` 문자열).

### 배경

레거시 `ComparisonService.getTransitTimes`([`ComparisonService.java:208`](/Users/hankyungjun/projects/knockdog_server/src/main/java/com/petcampus/knockdog/comparison/service/ComparisonService.java))는 (기준점 × 유치원) 쌍마다 도보/자동차/대중교통 3종을 `CompletableFuture`로 병렬 조회하고, 종류별로 Redis 문자열 캐시(`transit:{locationHash}:{kindergartenId}:{type}`, TTL 7일)를 먼저 확인한다. 도보·자동차는 [`TmapApiClient`](/Users/hankyungjun/projects/knockdog_server/src/main/java/com/petcampus/knockdog/client/TmapApiClient.java)(`POST /tmap/routes/pedestrian`, `POST /tmap/routes`, 응답 `features[].properties.totalTime`), 대중교통은 [`NaverMapApiClient`](/Users/hankyungjun/projects/knockdog_server/src/main/java/com/petcampus/knockdog/kindergardeninfo/client/NaverMapApiClient.java)(비공식 `pt.map.naver.com` endpoint, Jsoup으로 직접 호출)를 쓴다. 실패 시 예외를 던지지 않고 `time: null`로 채워 비교 화면 자체는 항상 뜨게 한다.

v2는 유치원 데이터가 RDB로 이관됐고(KD3-413), Redis는 현재 리프레시 토큰 전용(`RedisRefreshTokenEntity`, `@RedisHash`)이라 캐시 용도로 쓰는 건 이번이 처음이다. `spring-boot-starter-data-redis`는 이미 의존성에 있고 `spring.data.redis.host/port`도 설정돼 있어 `StringRedisTemplate`을 바로 주입받을 수 있다(레거시처럼 별도 `RedisConfig` 불필요).

## 작업 범위

- `kindergarten` 도메인에 이동시간 조회 아웃바운드 포트·TMAP 어댑터 추가.
- `CompareKindergartensService`가 (기준점 × 유치원) 쌍마다 이동시간을 조회해 `KindergartenComparisonResponse.transitTimes`에 채운다.
- Redis 캐싱(격자 해시 키, TTL 7일, 설정 가능).
- `application.yaml`에 `tmap.api.key`/`tmap.api.base-url`/`cache.transit.ttl-days` 환경변수 추가.
- `docs/inventory/integrations.md`의 TMAP·네이버 대중교통 행 갱신.
- `docs/domains/kindergarten.md`에 이동시간 연동 반영.

## 작업 제외 범위

- 비교 히스토리 저장(KD3-496, 완료).
- TMAP API 키 발급·쿼터 확인 — 사람 몫. 이 작업에서는 인터페이스만 구현하고 실키 없이 진행한다(§방향 논의).
- 프론트 `time` 포맷 변경 반영 — 프론트 저장소 작업.

## 방향 논의 및 결정 사항

### 확정 사항 (사용자 승인, 2026-09-14)

1. **대중교통 소스**: TMAP 대중교통 API(`POST https://apis.openapi.sk.com/transit/routes`)로 통합. 레거시 네이버 비공식 endpoint는 쓰지 않는다 — 인벤토리(`integrations.md`) `DEFER` 2건(TMAP/네이버 대중교통) 중 네이버 항목은 이 작업으로 해소(제거)된다.
2. **캐시 테스트 전략**: 레거시 패턴 답습 — CI에 Redis 서비스 컨테이너를 추가하지 않는다. `RefreshTokenCacheAdapter`와 동일하게 어댑터 자체의 Redis 연동은 직접 테스트하지 않고, 서비스·매핑 로직은 유닛 테스트(fake 포트)로, TMAP 어댑터의 HTTP 파싱·에러 처리는 `MockRestServiceServer`로 검증한다. 실제 캐시 히트/TTL 동작은 로컬 docker-compose Redis로 수동 확인.
3. **`transitTimes[].time` 포맷**: 초 단위 `number`로 전환(KD3-495 날짜·시간 컨벤션, KD3-496 `comparedAt`과 동일한 방향). 프론트는 레거시 문자열 대신 초 단위 숫자를 받아 자체 포맷팅해야 한다 — 별도 프론트 이관 항목.
4. **TMAP API 키**: 아직 미발급. 인터페이스·설정값(`tmap.api.key`)만 만들고 실키 없이 진행한다. 로컬/CI 검증은 mock으로 하고, 실제 TMAP 응답 검증은 키 발급 후 사람이 확인(Notion API 명세서 미등록 건과 같은 성격의 human-task로 문서화).

### 대중교통 API 응답 스키마 (TMAP, 2026-09-14 공식 문서 확인)

- `POST /transit/routes`, 헤더 `appKey`, 바디 `startX/startY/endX/endY/count/lang/format`.
- 응답 `metaData.plan.itineraries[]`, 각 항목 `totalTime`(초). 가장 앞 itinerary(추천 경로)를 사용한다.
- 도보/자동차는 레거시와 동일한 `POST /tmap/routes/pedestrian`, `POST /tmap/routes`, 응답 `features[].properties.totalTime`(초).

## 완료 확인 기준

### 테스트 (2026-09-14, `./gradlew clean test ktlintCheck` — 총 234개, 실패 0, ArchUnit 통과)

- `TmapTransitTimeAdapterTest` — `MockRestServiceServer`로 도보·자동차·대중교통 병렬 호출, TMAP 응답 파싱(`features[].properties.totalTime`, `metaData.plan.itineraries[].totalTime`), 캐시 히트 시 HTTP 미호출, 개별 교통수단 실패 시 해당 항목만 `null`, 204(경로 없음) 처리를 검증. 실제 Redis·TMAP 키는 쓰지 않는다(§방향 논의 결정 2, 4).
- `CompareKindergartensServiceTest` — 기준점 × 유치원 쌍마다 포트를 호출하는지, 좌표 없는 유치원은 호출하지 않는지 검증.
- `KindergartenComparisonResponseTest`/`KindergartenComparisonEndpointTest` — `transitTimes`가 기준점 순서대로 `{type, time}`(초 단위 number)으로 내려가는지 검증(엔드포인트 테스트는 fake 포트로 결정적으로 검증).

### REDESIGN 응답 대조 (`003-migration.md` §4는 `KEEP` 전용 — 이 필드는 REDESIGN이라 해당 없음)

- `transitTimes[].time`을 레거시 문자열(`"2시간 49분"`)에서 초 단위 `number`로 바꾸기로 확정했다(§방향 논의 결정 3). 계약을 바꾸는 결정이라 로컬 응답 대조 대상이 아니다.
- TMAP 실응답 검증은 API 키가 없어 수행하지 못했다(§방향 논의 결정 4) — `MockRestServiceServer`로 요청 형태·응답 파싱 로직만 검증했다. 키 발급 후 사람이 실제 TMAP 응답으로 재검증해야 한다.
- Redis 캐시의 실제 히트/TTL 동작은 CI에 Redis가 없어(§방향 논의 결정 2) 자동 검증하지 못했다 — 로컬 docker-compose Redis로 수동 확인이 필요하다(제한 사항으로 명시).

## 작업 후 확인 목록

- [ ] `docs/inventory/integrations.md` TMAP/네이버 대중교통 행 갱신
- [ ] `docs/domains/kindergarten.md` 이동시간 연동 반영
- [ ] TMAP API 키 발급·쿼터 확인 — 사람 몫(후속)
- [ ] 프론트 `transitTimes[].time` number 파싱 반영 — 프론트 저장소 작업(후속)
