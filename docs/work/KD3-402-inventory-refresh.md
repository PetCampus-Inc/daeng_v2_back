> 생성: 2026-08-31 15:51 · 최종 수정: 2026-08-31 15:51

# KD3-402 인벤토리 문서 레거시 기준 최신화

| 항목 | 값 |
|---|---|
| Jira | `KD3-402` |
| 브랜치 | `docs/KD3-402-inventory-refresh` |
| 상위 에픽 | `KD3-194` |

## 현재 제어점

- 활성 workflow: `008-docs`
- 현재 공통 단계: `5`
- 다음 결정 또는 전환 조건: PR 리뷰에서 판정(특히 `DEFER` 4건)이 승인되면 `epic/KD3-194-common-base`로 squash merge

## 작업 목표

`docs/inventory/`의 4개 문서를 읽는 사람이 **오늘의 레거시 상태**를 근거로 이관 판정을 내릴 수 있게 한다.

독자는 이 저장소에서 도메인 슬라이스를 착수하는 사람과 AI 에이전트다. 이들이 답을 얻어야 하는 질문은 "이 endpoint/테이블/연동을 신규 서버로 가져가야 하는가, 가져간다면 계약을 유지하는가"이며, 판정의 근거가 문서 안에 있어야 한다.

문서가 2026-08-04~05 스냅샷에 멈춰 있어, 그 이후 레거시에 추가된 대상이 문서에 아예 없었다. 목록에 없으면 판정이 없는 것과 구분되지 않아, 착수하는 사람이 매번 레거시를 다시 조사하게 된다.

## 작업 범위

### 갱신 대상 문서

[`docs/rules/documentation.md`](../rules/documentation.md) §1 배치 기준에 따라 아래 4개가 대상이다. 각 영역의 단일 기준 문서이므로 새 문서를 만들지 않고 기존 문서를 갱신한다.

- [`docs/inventory/api.md`](../inventory/api.md) — API 계약·인벤토리
- [`docs/inventory/database.md`](../inventory/database.md) — 데이터·이관
- [`docs/inventory/integrations.md`](../inventory/integrations.md) — 외부 연동
- [`docs/inventory/operations.md`](../inventory/operations.md) — 운영 전환

### 사실 근거

| 출처 | 기준 |
|---|---|
| 레거시 서버 | `PetCampus-Inc/daeng_v1_back` `dev@2479b02c` (2026-08-30) |
| 프론트 | `PetCampus-Inc/daeng_v2_front` `develop@b702973` (2026-08-31) |
| ERDCloud 초안 | 기존 문서에 기록된 `../똑독 V3-snapshot.json` (2026-08-04 추출). 이번에 재추출하지 않음 |

추출 방식:

- 레거시 endpoint — `**Controller.java`의 class-level `@RequestMapping` + method-level mapping annotation 조합. 주석 처리된 mapping 제외
- 레거시 데이터 — `@Entity` 클래스의 `@Table(name=…)`, 없으면 Spring 기본 네이밍(snake_case). 컬럼·제약 근거는 `scripts/migrations/*.sql` 22개
- 프론트 호출 — `apps/knockdog`의 `api.{get,post,put,patch,delete}(…)`와 `fetch('/api/v0/**')`. ky 클라이언트의 `prefixUrl`이 `/api/v0`이라 상대 경로를 복원해 대조

### 확인한 드리프트

문서의 마지막 추출 시점(2026-08-04~05) 이후 레거시 `dev`에 82커밋(`src/main` 71커밋), DB 마이그레이션 14개가 들어왔다.

## 작업 제외 범위

- 신규 서버 코드 변경. 이번 작업은 문서만 바꾼다
- ERDCloud 스냅샷 재추출. 초안 29개는 기존 기록을 그대로 두고 레거시 엔티티로 교차 확인만 했다
- 레거시 저장소의 평문 secret 로테이션. 사실은 `operations.md`에 기록했으나 조치는 별도 티켓이 필요하다 (아래 `미결 질문` 참고)
- 운영 인스턴스 실물 확인. `operations.md`에 적은 레거시 구성은 저장소에 커밋된 내용 기준이며, 3절에 그 한계를 명시했다

## 방향 논의 및 결정 사항

### 확정 사항

**판정 기준은 기존 문서의 규칙을 그대로 적용한다.** `api.md` §2는 "프론트/앱 호출 여부, 운영 사용 여부, 보안 위험 등 근거 없이 판정하지 않는다"고 정하고 있다. 신규 endpoint도 이 기준으로만 판정했다.

- 프론트 호출이 확인된 37개 → `KEEP` / `v0` / P1
- 호출처를 찾지 못한 3개 → `DEFER`. 프론트에 없다는 것만 확인했고 앱·운영 잔존 호출은 확인하지 못했으므로 `DROP`으로 내리지 않았다

**프론트 추출은 3가지 호출 형태를 모두 잡아야 한다.** 처음 문자열 리터럴만 추출했을 때 "프론트 미호출"로 나온 24개가 실제로는 호출 중이었다. 원인은 두 가지였다.

- 줄바꿈 체이닝 — `return await api\n  .post('pet/register', …)`
- 경로 상수 인자 — `api.post(ATTENDANCE_RECORDS_PATH, …)`

이 때문에 최종 대조에서 **프론트 호출 120개가 레거시 endpoint와 100% 매칭**(고아 호출 0)됐다. `apps/mobile`은 WebView 셸이라 백엔드를 직접 호출하지 않는다.

**기존 4개 행의 판정을 뒤집었다.** 프론트가 대체 endpoint로 이사했는데 문서가 따라가지 못한 경우다.

| 행 | 변경 | 근거 |
|---|---|---|
| `GET /api/v0/breed` | `KEEP` → `DEFER` | 프론트가 `GET /api/v0/breed-catalog`로 이전 (KD3-370) |
| `GET /api/v0/mypage/getPushSetting` | `KEEP` → `DEFER` | 프론트가 `GET /api/v0/notification-settings`로 이전 (KD3-338) |
| `POST /api/v0/mypage/updatePushSetting` | `KEEP` → `DEFER` | 프론트가 `PUT /api/v0/notification-settings`로 이전 (KD3-338) |
| `POST /api/v0/mypage/updateGuardianProfile` | `DEFER` → `KEEP` | 프론트가 실제 호출 중 (`src/entities/user/api/user.ts`) |

**`database.md`에 제외 근거 절을 신설했다.** 레거시 엔티티 59개 중 v1/v2 잔존 17개는 [`0001`](../adr/0001-legacy-v1-v2-폐기.md)에 따라 이관 대상이 아니지만, 기존 문서에는 제외했다는 기록 자체가 없었다. 인벤토리에서 "빠뜨린 것"과 "판정해서 뺀 것"을 구분할 수 없으면 다음 사람이 같은 조사를 반복한다. 4절에 행을 만드는 대신 5절에 근거를 남겼다.

**`tb_breed`를 `DROP`으로 확정했다.** 레거시가 KD3-370에서 `breed_catalog`를 신설해 대체했고 프론트도 이전을 마쳤다. 기존 `DEFER`의 후속 확인 항목("seed source, 운영 수정 여부")이 해소됐다.

**각 문서에 기준 커밋 SHA를 남기는 규칙을 §2에 추가했다.** 이번 드리프트의 직접 원인은 문서가 "최종 수정 2026-08-30"으로 보이면서 실제 추출 기준은 08-04였다는 점이다. SHA가 없으면 다음 사람이 무엇이 반영됐는지 알 수 없다.

**`api.md` 3절의 프론트 경로를 저장소 식별자로 교체했다.** 기존에는 특정인의 로컬 절대경로(`/Users/…`)여서 다른 사람이 재현할 수 없었다.

### 미결 질문

- **`DEFER` 3건(`breed`, `getPushSetting`, `updatePushSetting`)을 언제 `DROP`으로 확정할 것인가.** 프론트 미호출만 확인했다. 운영 로그에서 실제 트래픽이 0인지 확인하는 주체와 시점이 필요하다.
- **`DEFER` 3건(`GET /attendance-checkinouts/{petId}`, `GET /pet/{petId}`, `DELETE /push-devices/{pushDeviceId}`)의 호출 주체.** 같은 컨트롤러의 다른 endpoint는 모두 프론트가 쓰는데 이것만 호출처가 없다. 기획상 예정된 것인지 미사용인지 확인이 필요하다.
- **`user_notification_setting`과 `notification_preference` 중 어느 쪽이 진실인가.** 레거시는 KD3-287에서 앞의 테이블을 건드리지 않고 뒤의 테이블을 새로 만들어 둘이 공존한다. 신규 서버는 하나로 합쳐야 하며, 어느 쪽 데이터를 기준으로 삼을지 결정이 필요하다.
- **레거시 평문 secret 로테이션 티켓.** `application.yml`·`docker-compose.yml`에 JWT 서명키·DB 계정·메일 계정·외부 API key가 평문으로 커밋돼 있고 이력에 남아 있다. `operations.md`에 사실과 위험은 기록했으나(값은 기록하지 않음) 실제 로테이션은 이 티켓 범위 밖이다. 대상과 담당을 정할 별도 티켓이 필요하다.

### 사용자 승인 기록

- 2026-08-31 · 조사 결과 보고 후 사용자가 갱신 범위를 승인. 프론트 호출 확인이 필요한 항목에 대해 저장소 경로(`PetCampus-Inc/daeng_v2_front`)를 제공받아, 미확인 상태로 `DEFER` 처리하는 대안 대신 실제 호출 대조 후 판정을 확정하는 방향을 선택
- 2026-08-31 · 별도 PR로 `epic/KD3-194-common-base`에 머지하도록 요청받음. Jira 티켓이 없어 확인한 결과, 사용자가 AI의 티켓 생성을 승인해 `KD3-402` 생성

## 완료 확인 기준

| 기준 | 결과 |
|---|---|
| `node scripts/docs-check.mjs` 통과 | 통과 (문서 38개) |
| 인벤토리 4개 문서의 표 열 수 정합 | 불일치 0건 |
| 인벤토리 4개 문서의 상대 링크 유효성 | 깨진 링크 0건 |
| `api.md` 4절 요약 수치가 5절 표와 일치 | 일치 (전체 259 / 프론트 확인 116 / KEEP 89 / REDESIGN 26 / DROP 137 / DEFER 7) |
| 레거시 endpoint 259개가 모두 인벤토리에 존재 | 누락 0건 |
| 인벤토리 행 중 레거시에 없는 endpoint | 0건 |
| 프론트 호출 120개가 모두 레거시 endpoint와 매칭 | 매칭 120/120, 고아 호출 0건 |
| 레거시 엔티티 59개가 인벤토리 또는 제외 절에 존재 | 42행 + 제외 17개 = 59, 누락 0건 |
| 문서에 secret 값 미포함 | 값 없음. 노출 사실과 위험만 기록 |

**확인하지 못한 것:**

- 운영 인스턴스의 실제 상태. `operations.md`의 레거시 구성은 저장소 커밋 내용 기준이며 3절에 이 한계를 적었다
- 앱·운영 트래픽 기준의 endpoint 사용 여부. 프론트 저장소 정적 분석만 했다
- ERDCloud 초안의 최신 여부. 2026-08-04 추출본을 그대로 뒀다

## 작업 후 확인 목록

| 문서 | 결과 | 근거 |
|---|---|---|
| `docs/service.md` | 해당 없음 | 제품 범위·단계 변화 없음 |
| `docs/rules/` | 해당 없음 | 저장소 규칙 변경 없음. 인벤토리 §2의 갱신 규칙 추가는 각 인벤토리 문서 내부 규칙 |
| `docs/workflows/` | 해당 없음 | 절차 변경 없음 |
| `docs/architecture/` | 해당 없음 | 신규 서버 구조 변화 없음 |
| `docs/conventions/` | 해당 없음 | 코드 작성 기준 변화 없음 |
| `docs/adr/` | 확인했지만 변경 없음 | `0001`, `0004`, `0005`, `0010`의 결정을 판정 근거로 인용만 했고 결정 자체는 바뀌지 않았다 |
| `docs/domains/auth.md` | 확인했지만 변경 없음 | `user_agreement`, `notification_preference` 등 신규 테이블은 아직 소유 도메인 후보 단계다. 도메인 슬라이스 착수 시 확정한다 |
| `docs/inventory/api.md` | 갱신 | 259행으로 재작성, 요약·추출 범위 갱신 |
| `docs/inventory/database.md` | 갱신 | 42행으로 확장, 제외 근거 절 신설 |
| `docs/inventory/integrations.md` | 갱신 | 연동 13개로 확장 |
| `docs/inventory/operations.md` | 갱신 | `DEFER` 6건 해소, 항목 2개 추가 |
| Notion API 명세 | 해당 없음 | 계약 자체는 바뀌지 않았다. 신규 endpoint의 상세 명세는 도메인 슬라이스 착수 시 [`notion-api-spec-sync.md`](../rules/notion-api-spec-sync.md)에 따라 작성한다 |
