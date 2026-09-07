> 생성: 2026-09-02 19:24 · 최종 수정: 2026-09-07 20:35

# KD3-432 pet 목록·단건 조회 API 구축

| 항목 | 값 |
|---|---|
| Jira | `KD3-432` |
| 브랜치 | `feat/KD3-432-pet-profile-query-api` (2026-09-07 생성, 사용자 지시로 AI가 생성 — 통상 절차와 달리 사람이 Jira에서 미리 만들어 제공하지 않음) |
| 상위 에픽 | `KD3-404` |

## 현재 제어점

- 활성 workflow: `003-migration`
- 현재 공통 단계: `4`
- 다음 결정 또는 전환 조건: 작업 브랜치는 `dev`/`epic`이 아니라 `feat/KD3-431-pet-profile-create-update-api`에서 분기한 stacked 브랜치다(KD3-431이 epic에 머지되면 base 재조정·rebase 예정). 구현·단위 테스트·전체 빌드·자체 재검토(2건 정정) 및 `MethodArgumentTypeMismatchException` 공통 핸들러 추가까지 완료(148건 통과)하고 커밋 4개로 나눠 커밋·푸시, PR #20 생성 완료(base `feat/KD3-431-pet-profile-create-update-api`, `MERGEABLE`). 로컬 MySQL 실제 HTTP 엔드투엔드 검증도 완료(§검증 결과 참고). 다음은 5단계 독립 리뷰.

## 작업 목표

인증된 사용자가 자신의 활성 pet 목록과 단건 프로필을 조회할 수 있는 v1 API를 제공한다.

## 작업 범위

- `GET /api/v1/pets` 목록 조회 API를 구현한다.
- `GET /api/v1/pets/{petId}` 단건 조회 API를 구현한다.
- 본인 소유와 활성 상태를 검증한다.
- 목록 정렬과 반환 DTO를 v1 제품 요구에 맞춰 명세·테스트한다.

## 작업 제외 범위

- `schoolConnectionBadge`와 유치원 연결 상태
- 원장·유치원 구성원의 pet 조회 권한
- 레거시 v0 목록·단건 조회 API 호환

## 방향 논의 및 결정 사항

### 확정 사항

- 이번 단계의 조회 주체는 pet 소유자인 인증 사용자만이다.
- 유치원/owner-member는 다른 담당 범위이며 응답과 조회 조건에서 제외한다.
- 신규 API는 v1 REST 경로만 제공한다.
- 작업 브랜치는 `dev`가 아닌 `feat/KD3-431-pet-profile-create-update-api`에서 분기한 stacked 브랜치로 진행한다. 이 티켓의 조회 API가 KD3-431의 `LoadBreedPort`(breed 표시 이름 조합)·`PetResponse`(응답 DTO)·`PetErrorCode`(소유권·미존재 에러)에 기능적으로 의존하기 때문이다. PR base는 KD3-431이 `epic/KD3-404-pet-domain-migration`에 머지되기 전까지 `feat/KD3-431-pet-profile-create-update-api`로 두고, 머지 후 재조정한다.
- **목록(`GET /api/v1/pets`) 기본 정렬은 "대표견 우선 → 나머지는 이름 오름차순(Java 기본 `String` 비교)"으로 확정한다.** 레거시(`daeng_v1_back`의 `PetService.getPets()`, 커밋 `8dcaee89`, KD3-299)의 정렬 규칙("대표 강아지 → 연결된(ACTIVE) 강아지 가나다순 → 미연결 강아지 가나다순")을 근거로 확인했다 — 2·3단계(유치원 연결 여부 기준)는 이 티켓의 제외 범위(유치원 연결 상태)와 겹쳐 적용할 데이터가 없으므로 자연히 빠지고, 범용 규칙인 1단계(대표견 우선)만 남는다. 그 뒤 이름 정렬은 레거시와 동일하게 별도 로케일 처리 없이 `String`의 기본 비교(자연 순서)를 그대로 쓴다.
  - **완성형 한글 이름은 기본 `String` 비교로 가나다순이 정확히 나온다.** 완성형 음절 블록(U+AC00~U+D7A3)은 (초성, 중성, 종성) 조합 순으로 코드가 배정돼 있고, 이 초성 순서가 한국어 사전 자음 순서(ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ)와 일치한다.
  - **"완성형 음절이 아니라 낱자모(ㄱㄴㄷ 등)로 시작하는 이름"은 코드값만으로는 순서가 어긋난다** — 이건 이 프로젝트만의 문제가 아니라 유니코드 기술위원회 문서가 명시한 알려진 한계다. Unicode Technical Committee 문서(L2/17-078, "Hangul Sort Order")는 "유니코드 코드값만으로 한글 자모를 정렬하는 것은 확실히 좋은 방법이 아니며 피해야 한다(sorting hangul jamo solely according to their Unicode code points is definitely not a good idea and should be avoided)"고 명시하고, 실제 한국어 화자가 기대하는 순서를 내려면 한국 국가표준 KS X 1026-1을 따라야 한다고 밝힌다. 즉 표준 도구(JDK의 `Collator.getInstance(Locale.KOREAN)`, 직접 실행해 확인)조차 이 문제를 기본값으로 해결해주지 않는다 — KS X 1026-1 기준 커스텀 정렬 규칙을 별도로 구현해야만 완전히 해결되는 문제이지, 라이브러리 설정 하나로 되는 게 아니다.
  - **그래서 이번 범위(최대 5마리)에서는 이 한계를 그대로 받아들인다.** 레거시가 동일한 방식(낱자모 예외 미처리)으로 이미 프로덕션에서 운영 중이었고(git 히스토리상 이 이슈로 인한 버그 수정 이력 없음), 완전한 해결은 KS X 1026-1 기반 커스텀 비교 로직을 새로 구현해야 하는 별도 작업이라 이번 티켓 범위에 맞지 않는다. 참고: [Unicode L2/17-078 "Hangul Sort Order"](https://www.unicode.org/L2/L2017/17078-hangul-sort-order.pdf), [나무위키 "정렬/순서"](https://namu.wiki/w/%EC%A0%95%EB%A0%AC/%EC%88%9C%EC%84%9C)(음절·자소 단위 비교 차이의 일반적 설명).

### 구현 중 발견해 정정한 사항

- **breed 조회 실패 시 `requireNotNull`(→400)로 짰다가 `checkNotNull`(→500)로 정정했다.** `GetPetsService`/`GetPetService`가 이미 저장된 pet의 `breedId`로 breed를 조회할 때, 그 breed가 없는 경우는 이번 요청이 잘못 보낸 게 아니라(요청은 인증 정보·`petId`만 주고 `breedId`는 아예 관여하지 않는다) 과거에 저장된 데이터 정합성이 깨진 서버 쪽 문제다 — `require`(값 문제, 400)가 아니라 `check`(상태/정합성 문제, 500) 계열을 써야 클라이언트에게 잘못된 책임을 지우지 않는다. `CreatePetService`/`UpdatePetService`의 `loadBreedPort.findById(command.breedId) ?: NOT_FOUND_BREED`(400)와는 다른 경우다 — 그쪽은 `breedId`가 이번 요청의 body에서 직접 온 값이라 진짜 클라이언트 입력 오류가 맞다.
- **조회 서비스에 `@Transactional(readOnly = true)`가 빠져 있던 것을 추가했다.** breed 도메인의 `BreedQueryService`가 이미 이 컨벤션을 쓰고 있는데(조회 서비스는 `readOnly = true` 트랜잭션으로 감싼다) 놓쳤다가 자체 재검토로 발견해 `GetPetsService.getPets()`/`GetPetService.getPet()`에 추가했다. `GetPetsService`는 특히 pet 목록 조회 1번 + breed 조회 최대 5번을 하나의 읽기 트랜잭션으로 묶어야 일관된 스냅숏을 보장한다.
- **`GlobalExceptionHandler`에 `MethodArgumentTypeMismatchException` → 400 핸들러를 추가했다(pet 범위를 넘는 공통 변경).** `GetPetController`의 `@PathVariable petId: Long`처럼 경로 변수 타입이 `Long`인데 숫자가 아닌 값(`/api/v1/pets/abc`)이 오면 Spring이 이 예외를 던지는데, 전용 핸들러가 없어 이 클래스의 catch-all(`Exception::class`)로 떨어져 500으로 응답하고 있었다. 이건 KD3-431의 `UpdatePetController`도 이미 갖고 있던 기존 한계였다. 앞서 철회한 `IllegalStateException` 전역 핸들러와는 성격이 다르다고 판단해 이번엔 전역으로 추가했다 — 그건 "클라이언트 충돌 vs 내부 버그"를 도메인마다 다르게 판단해야 해서 전역 처리가 위험했지만, 이건 "경로 변수 타입이 안 맞으면 400"이 어떤 리소스든 예외 없이 항상 맞는 기계적 규칙이라 도메인 판단이 필요 없다. 또한 이 프로젝트의 catch-all이 없었다면 Spring이 원래 자동으로 400 처리해줬을 것을 catch-all이 가로채 500으로 만들어버리고 있었던 것이라, 새 기능이 아니라 프레임워크 기본 동작을 되살리는 수정이다. 상세 근거는 `docs/conventions/error-handling.md` §3에 기록.

### 미결 질문

- 없음. 아래 확정 사항 참고.

### 사용자 승인 기록

- 2026-09-02: 사용자가 본인 소유 pet 목록·단건 조회를 pet 범위에 포함하고 유치원 연계를 후속으로 분리했다.
- 2026-09-07: 사용자가 목록 기본 정렬 기준(대표견 우선 → 이름순, 레거시·Unicode 기술위원회 문서 근거)을 확정하고 구현 착수를 승인했다.

## 완료 확인 기준

- 목록·단건의 정상 경로, 타인 pet 접근 거부, 삭제된 pet 미노출을 테스트한다.
- 목록 정렬과 응답 계약을 명세·검증한다.
- API 인벤토리와 pet 도메인 문서 영향을 판정·기록한다.

## 검증 결과

- **`./gradlew build --rerun-tasks`(2026-09-07)**: ktlint, 컴파일, 전체 테스트, ArchUnit(헥사고날 경계) 통과. `GetPetsServiceTest` 4건(breed 정보 포함 반환, 대표견 우선·이름순 정렬, 빈 목록, 미존재 사용자), `GetPetServiceTest` 4건(정상 반환, 미존재 pet, 삭제된 pet, 타인 소유) 신규 통과, 기존 테스트 전부 회귀 없이 통과(총 147건, 실패·에러 0건).
- **완료 확인 기준 대조**: 정상 경로·타인 pet 거부·삭제된 pet 미노출(단건)은 위 단위 테스트로 검증했다. 목록의 "삭제된 pet 미노출"은 `GetPetsService`가 `LoadPetPort.findAllActiveByUserId`(KD3-430에서 이미 `deletedAt is null` 필터링과 함께 검증된 포트)에 위임하므로 별도 재검증하지 않았다.
- **로컬 HTTP 엔드투엔드 검증은 처음엔 위험도가 낮다고 판단해 생략했으나, 사용자 지적(단위 테스트는 fake port라 실제 Spring 라우팅·Jackson 역직렬화까지는 검증 못 한다)으로 실제로 수행했다(2026-09-07)**: 로컬 MySQL 데이터를 초기화(예전 세션의 Flyway 이력 충돌)하고 V1~V11 재적용, 테스트 사용자 2명으로 액세스 토큰을 서명해 검증했다(테스트 데이터는 검증 후 삭제):
  - `GET /api/v1/pets` 인증 없이 호출 → 401, 빈 목록 → 200 `data: []` 확인
  - pet 2마리 등록 후 목록 조회 → 대표견이 먼저, 나머지는 이름 오름차순(`나비` → `다롱이` → `라온이` → `마루`)으로 정렬됨을 실제 데이터로 확인
  - `GET /api/v1/pets/{petId}` 정상 단건 조회, 존재하지 않는 `petId` → 404 `PET-404-1`, 타인 소유 pet → 403 `PET-403-1` 확인
  - `GET /api/v1/pets/abc`(숫자 아닌 경로 변수) → 400 `INVALID_INPUT_VALUE` 확인 — 이번에 추가한 `MethodArgumentTypeMismatchException` 핸들러가 실제 요청에서도 500이 아니라 400을 응답함을 확인
- **자체 재검토 후 재검증(2026-09-07)**: §구현 중 발견해 정정한 사항의 `requireNotNull`→`checkNotNull` 정정, `@Transactional(readOnly = true)` 누락 추가 반영 후 `./gradlew build --rerun-tasks` 재실행 — ktlint, 컴파일, 전체 테스트, ArchUnit 통과, 기존 147건 그대로 유지(실패·에러 0건, 이번 정정은 예외 타입·트랜잭션 경계만 바꿔 테스트 케이스 자체는 늘지 않음).
- **`MethodArgumentTypeMismatchException` 핸들러 추가 후 재검증(2026-09-07)**: `GlobalExceptionHandler`에 핸들러 추가, `GlobalExceptionHandlerTest`에 `경로 변수 타입 불일치는 500이 아니라 400으로 응답한다` 케이스 추가 후 `./gradlew build --rerun-tasks` 재실행 — 29개 클래스 148건(기존 147 + 신규 1), 실패·에러 0건 확인.

## 작업 후 확인 목록

| 대상 | 판정 | 근거 |
|---|---|---|
| `docs/work/KD3-432-pet-profile-query-api.md` | 갱신 | API 결정·구현·검증 결과 기록 |
| `docs/inventory/api.md` | 갱신 | `/api/v0/pet/list`(`KEEP`→`REDESIGN`), `/api/v0/pet/{petId}`(`DEFER`→`REDESIGN`) 판정 정정, v1 엔드포인트 구현 완료 기록 |
| `docs/domains/pet.md` | 갱신 | "pet 목록·단건 조회 API" 절 신규 추가(정렬 규칙·근거·알려진 한계 포함) |
| `GetPetsUseCase.kt`/`GetPetUseCase.kt` | 신규 | 유스케이스 인터페이스(`GetBreedsUseCase` 네이밍 전례 따름) |
| `GetPetsService.kt`/`GetPetService.kt` | 신규 | 목록·단건 조회 서비스. 기존 `LoadPetPort`/`LoadBreedPort`/`PetErrorCode` 재사용, 신규 포트 없음. 자체 재검토로 `checkNotNull` 정정·`@Transactional(readOnly = true)` 추가 |
| `GetPetsController.kt`/`GetPetController.kt` | 신규 | `GET /api/v1/pets`, `GET /api/v1/pets/{petId}` |
| `GetPetsServiceTest.kt`/`GetPetServiceTest.kt` | 신규 | 정상 경로·정렬·빈 목록·미존재/삭제/타인 소유 케이스 |
| `GlobalExceptionHandler.kt`/`GlobalExceptionHandlerTest.kt` | 코드 추가 | `MethodArgumentTypeMismatchException` → 400 핸들러 추가(pet 범위를 넘는 공통 변경, 근거는 `docs/conventions/error-handling.md` §3) |
| `docs/conventions/error-handling.md` | 갱신 | `MethodArgumentTypeMismatchException` 처리 순위 추가, "새 핸들러는 catch-all을 고치지 말고 목록에 추가하는 방식을 따른다" 원칙 명문화 |
