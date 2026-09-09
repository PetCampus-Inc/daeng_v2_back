> 생성: 2026-09-08 17:15 · 최종 수정: 2026-09-08 22:20

# KD3-495 서버 응답 날짜·시간 포맷 통일

| 항목 | 값 |
|---|---|
| Jira | `KD3-495` |
| 브랜치 | `feat/KD3-495-response-datetime-format` |
| 상위 에픽 | `KD3-272` (유치원 도메인 마이그레이션) |

## 현재 제어점

- 활성 workflow: `003-migration`
- 현재 공통 단계: `5` (독립 리뷰·PR·문서 동기화)
- 다음 결정 또는 전환 조건: PR #24 리뷰/CI 통과 → dev 머지

## 작업 목표

신규 서버의 모든 API 응답에서 날짜·시간 필드를 하나의 포맷으로 통일한다.

| 개념 | 응답 DTO 타입 | JSON |
|---|---|---|
| 날짜 + 시각 | `LocalDateTime` | `"2026-09-05T11:38:48"` (초 항상 표기) |
| 날짜만 | `LocalDate` | `"2026-09-05"` |
| 시각만 | `LocalTime` | `"17:59"` / `"17:59:30"` (초가 0이면 생략) |

- 오프셋(`+09:00`)·`Z` 없음. 국내 전용 서비스라 전부 KST 벽시계, 프론트도 KST로 해석(사용자 결정, 2026-09-08).
- 소수점 이하(밀리·나노초)는 버린다.
- `LocalDateTime`은 타임스탬프라 초를 항상 붙여 폭을 고정하고, `LocalTime`은 분 단위 설정값(영업시간 등)이라 초가 0이면 생략한다(사용자 결정, 2026-09-08).
- 규약을 Jackson 직렬화 설정과 `docs/conventions/api-contract.md`에 못박고, 회귀 방지 테스트로 강제한다.

### 배경

- 레거시(`knockdog_server`)는 `LocalDateTime`을 배열(`[2026,9,5,11,38,48,나노초]`)로 직렬화해 내려줬다. 프론트(`daeng_v2_front`)는 `comparedAt: number[]` 같은 타입으로 이 배열을 받아 직접 파싱한다.
- v2에는 아직 응답에 날짜·시간 필드를 쓰는 DTO가 없다(영업시간을 `KindergartenDetailResponse`가 `"HH:mm"` 문자열로 손수 포맷하는 것이 유일). `LocalDateTime`은 도메인 내부·계산 로직에만 쓰인다.
- 첫 사용처는 `KD3-496`(비교 히스토리)의 `comparedAt`이다. 그 전에 서버 전역 규약을 세운다.

### 시간대 — 앱 코드가 아니라 배포에서 고정

`LocalDateTime`/`LocalTime`은 시간대 정보가 없어, `now()`류가 KST로 찍히는지는 JVM 기본 시간대에 달려 있다. 이걸 앱 코드에서 `TimeZone.setDefault`로 강제하는 것은 전역 상태 변경이라 지양하고, 배포 환경 설정으로 처리한다(사용자 결정, 2026-09-08).

- **운영**: 배포 컨테이너에 `TZ=Asia/Seoul` 주입(필수). `docs/inventory/operations.md`에 배포 필수 항목으로 기록.
- **테스트**: `build.gradle.kts`의 test 태스크가 `user.timezone=Asia/Seoul`로 고정. CI 러너(`ubuntu-latest`)는 UTC라, 이게 없으면 `now()` 기반 테스트가 로컬(KST)과 다르게 굴러간다.
- 앱 코드는 시간대를 만지지 않는다.

## 작업 범위

1. **Jackson 직렬화 포맷 고정** — `global/config/JacksonDateTimeConfig.kt`
   - `Jackson2ObjectMapperBuilderCustomizer` 빈에서 `WRITE_DATES_AS_TIMESTAMPS` 비활성.
   - `LocalDateTime` → `yyyy-MM-dd'T'HH:mm:ss`, `LocalDate` → `yyyy-MM-dd` 고정.
   - `LocalTime` → 초 단위로 잘라, 초가 0이면 `HH:mm`, 아니면 `HH:mm:ss`로 직렬화하는 커스텀 serializer.
   - `application.yaml`에 `spring.jackson.time-zone: Asia/Seoul`, `serialization.write-dates-as-timestamps: false` 명시(회귀 방지).
2. **테스트 시간대 고정** — `build.gradle.kts` test 태스크에 `systemProperty("user.timezone", "Asia/Seoul")`.
3. **컨벤션 문서화** — `docs/conventions/api-contract.md` §2에 날짜·시간 규약: 타입별 포맷, KST 기준, `Instant`/`OffsetDateTime` 등 금지, 강제 수단.
4. **회귀 방지 테스트**
   - `ResponseDateTimeFormatTest` — 커스터마이저가 `LocalDateTime`(나노초 포함)/`LocalDate`/`LocalTime`(초 0, 초 있음, 나노초)을 규약대로 직렬화하는지.
   - `ResponseDateTimeWiringTest`(`@SpringBootTest`) — 애플리케이션 컨텍스트가 실제 주입하는 `ObjectMapper`가 규약대로 직렬화하는지. 커스터마이저의 `serializers(...)` 등록이 빠지면 실패.
5. **운영 인벤토리 반영** — `docs/inventory/operations.md` 애플리케이션 배포 행에 `TZ=Asia/Seoul` 주입을 **필수 항목**으로 기록.

## 작업 제외 범위

- 기존 응답 DTO의 날짜 필드 마이그레이션 — 대상 없음. `KindergartenDetailResponse`가 `LocalTime`을 손수 `"HH:mm"`로 포맷하는 부분은 이제 규약과 결과가 같으므로 굳이 안 건드린다(KD3-496 등에서 정리 가능).
- 요청(request) 파라미터·바디의 날짜 파싱 규약 — 현재 날짜 입력 API 없음.
- 영속성 계층 타입 변경 — DB는 `DATETIME`/`TIME`, 도메인·DTO는 `java.time` 로컬 타입 유지.
- 도메인 로직의 `Clock` 주입/테스트 시간 고정 리팩터링.
- 앱 코드에서의 JVM 시간대 강제 — 배포/빌드 설정으로 처리(위 참조).
- 프론트(`daeng_v2_front`) 수정 — 소비처가 생기는 `KD3-496`에서 함께 조정.

## 방향 논의 및 결정 사항

### 확정 사항

- 응답 날짜·시각은 오프셋 없는 로컬 포맷, KST 기준.
- 타입별 포맷: `LocalDateTime` `yyyy-MM-dd'T'HH:mm:ss`(초 고정) / `LocalDate` `yyyy-MM-dd` / `LocalTime` `HH:mm` 또는 `HH:mm:ss`(초 0이면 생략), 나노초 버림.
- `Instant`/`OffsetDateTime`/`ZonedDateTime`/`java.util.Date`는 응답 필드로 쓰지 않는다.
- JVM 시간대는 배포 `TZ=Asia/Seoul` + 빌드 `user.timezone`으로 고정. 앱 코드는 관여 안 함.

### 미결 질문

1. **ADR로 남길지** — **해소(2026-09-08)**: 별도 ADR 없이 `api-contract.md` + 이 문서로 간다.
2. **시간대 고정 방식** — 처음엔 앱 코드 `TimeZone.setDefault` + 가드로 갔으나, 전역 상태 변경이 지저분하다는 판단으로 **배포 `TZ` + 빌드 `user.timezone`으로 전환(2026-09-08, 사용자 결정)**. 앱 코드에서 `DefaultTimeZoneConfig` 제거.

### 사용자 승인 기록

- 2026-09-08 — 작업 문서 검토 후 승인("진행").
- 2026-09-08 — 독립 리뷰 지적 M1/L3/L4 반영, `feat → dev` PR(#24) 생성 승인.
- 2026-09-08 — 리뷰 후속 논의로 (a) 시간대 고정을 앱 코드에서 배포/빌드 설정으로 전환, (b) `LocalTime` 규약(`HH:mm`/`HH:mm:ss`) 추가 확정. PR #24 수정 승인.

### 독립 리뷰

컨텍스트를 공유하지 않는 리뷰어가 커밋 `58e42ba`와 이 문서를 대조(2026-09-08).

| 지적 | 처리 |
|---|---|
| M1 — 시간대 고정 구현이 작업 범위 §1과 불일치(`@PostConstruct` 하나로 뭉침), 순서 불확정 | 후속 논의에서 앱 코드 시간대 고정 자체를 **제거**하고 배포 `TZ` + 빌드 `user.timezone`으로 전환. §1·§배경 갱신 |
| L3 — `api-contract.md` 강제수단에 `ResponseDateTimeWiringTest` 누락 | 추가 |
| L4 — §4 "jsr310 모듈 빠지면 실패" 문구 부정확 | 문구 수정(명시 등록이라 무관) |
| L1 — 응답 DTO 시간 타입 제한 강제 수단 없음 | `api-contract.md`에 금지 명시 + ArchUnit 규칙은 후속 티켓 |
| L2 — `TimeZone.setDefault` 전역 상태 오염 | 앱 코드에서 제거되어 해소. 테스트는 `build.gradle.kts`가 프로세스 시작 시 `user.timezone` 고정(런타임 변경 아님) |

빌드/컨벤션/커밋/문서 배치는 리뷰에서 이상 없음 확인.

### 후속 작업

- **응답 DTO 시간 타입 제한 ArchUnit 규칙** — `adapter/inbound/web` 응답 DTO가 `Instant`/`OffsetDateTime`/`ZonedDateTime`/`java.util.Date`를 필드로 쓰지 못하게 강제. 지금은 `api-contract.md`에 규약만 있고 강제 수단이 없다. 별도 티켓.

## 완료 확인 기준

- `./gradlew clean test ktlintCheck` 통과 — 2026-09-08 확인. 총 86개 테스트, 실패 0.
- `ResponseDateTimeFormatTest`:
  - `LocalDateTime` 나노초 포함 → `"2026-09-05T11:38:48"`, 초 0 → `"2026-01-02T03:04:00"`, `LocalDate` → `"2026-09-05"`.
  - `LocalTime` 초 0 → `"17:59"`, 초 있음 → `"17:59:30"`, 나노초 → 버림(`"17:59:30"`).
- `ResponseDateTimeWiringTest`(`@SpringBootTest`) — 컨텍스트가 주입하는 `ObjectMapper`가 초 단위 문자열로 직렬화. 커스터마이저 `serializers(...)` 제거 시 `"...48.123456789"`로 실패 확인(RED 검증).
- `@SpringBootTest` 컨텍스트 로드 통과 — `JacksonDateTimeConfig` 빈 정상 배선.
- 로컬 docker 기동 HTTP 대조는 수행하지 않음 — API 엔드포인트 없음(`KEEP` 대상 없음), 배선은 `@SpringBootTest`로 검증.
- `docs/conventions/api-contract.md` §2, `docs/inventory/operations.md` 배포 행 `TZ=Asia/Seoul` 필수 항목, `build.gradle.kts` test 태스크 `user.timezone` 확인.

## 작업 후 확인 목록

- `docs/conventions/api-contract.md` — 갱신함(§2 날짜·시간 규약 신설, 기존 §2 참고 → §3).
- `docs/inventory/operations.md` — 갱신함(애플리케이션 배포 행에 `TZ=Asia/Seoul` 필수 주입).
- `docs/adr/` — 해당 없음(미결 질문 1 해소).
- `docs/domains/*` — 해당 없음.
- `docs/inventory/api.md` — 해당 없음(경로·계약 판정 변화 아님).
