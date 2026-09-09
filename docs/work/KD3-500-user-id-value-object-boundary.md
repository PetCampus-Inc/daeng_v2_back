> 생성: 2026-09-09 17:30 · 최종 수정: 2026-09-09 (PR 리뷰 반영: 범위·제외범위 정합성, 검증 근거 정정)

# KD3-500 pet 사용자 식별자 타입 경계 통일

| 항목 | 값 |
|---|---|
| Jira | `KD3-500` |
| 브랜치 | `refactor/KD3-500-jwt-principal-user-id` |
| 상위 에픽 | 해당 없음 |

## 현재 제어점

- 활성 workflow: `003-migration`
- 현재 공통 단계: `4` (구현·검증 완료)
- 다음 결정 또는 전환 조건: 독립 리뷰·로컬 e2e·테스트 갭 보강까지 전부 완료 — PR 생성만 남음

## 작업 목표

pet 서비스에서 사용자 조회 결과의 내부 식별자를 원시 `Long`으로 즉시 풀지 않고 `UserId`로 유지한다. pet application/domain/port도 `UserId`를 사용하고, JPA repository/entity가 요구하는 영속성 어댑터 경계에서만 `.value`를 꺼낸다.

반려견 출생 연도는 값이 입력된 경우 현재 연도 기준 최근 30년 이내로 제한한다.

## 작업 범위

- `CreatePetService`, `UpdatePetService`, `GetPetService`, `GetPetsService`, `SetRepresentativeService`, `DeletePetService`의 `requireUserId` 반환 타입을 `UserId`로 변경한다.
- `Pet.userId`, `LoadPetPort`, `PetLockOperations`, `LockUserPort`의 사용자 식별자 파라미터를 `UserId`로 변경한다.
- controller에서 인증 principal로 받은 `UserCode`와 이를 전달하는 pet command는 유지하고, service의 사용자 조회 결과부터 `UserId`를 사용한다.
- `PetMapper`, `PetPersistenceAdapter`, JPA repository/entity가 요구하는 영속성 경계에서만 `UserId.value`를 전달한다.
- pet 도메인·서비스·영속성 테스트 fixture를 `UserId` 기준으로 전환하고, 전체 빌드로 회귀를 검증한다.
- `Pet.create`와 `Pet.update`에 출생 연도 범위 검증을 추가한다.
- `requireUserId(userCode: UserCode): UserId` 중복(6개 pet 서비스 + auth `UserAgreementService`, 총 7곳에 토씨 하나 안 틀리고 복붙됨)을 `auth.application.service.RequireUserId` 공유 컴포넌트로 추출한다.

## 작업 제외 범위

- JWT subject, Spring Security principal, access/refresh token, Redis refresh token 구조 변경
- controller·command의 공개 인터페이스 변경(HTTP 요청·응답 계약은 그대로 유지)
- `UserId`를 auth 패키지 밖 shared kernel로 이동하는 패키지 재구성

포트(`LoadPetPort`/`PetLockOperations`/`LockUserPort`)의 사용자 식별자 파라미터 타입(`Long` → `UserId`)은 위 "작업 범위"에 명시한 대로 이번 작업에 **포함**된다 — 포트도 "공개 인터페이스"이므로 제외 대상처럼 읽힐 수 있었던 이전 문구를 정정했다. 여기서 "포트·컨트롤러·커맨드의 공개 인터페이스를 안 바꾼다"는 건 HTTP 요청·응답 계약(controller/command)에 한정된 얘기였다.

## 방향 논의 및 결정 사항

### 확정 사항

- `UserId`는 현재 `Long`을 감싸지만, 사용자 식별자라는 타입 정보를 서비스 내부에서 유지한다.
- pet domain/application/port에서 `UserId`를 유지하고, JPA repository/entity에 연결되는 persistence adapter에서만 `.value`를 사용한다.
- `birthYear`는 nullable이지만 값이 있으면 `현재 연도 - 30` 이상, 현재 연도 이하여야 한다.
- `UserId`는 현재 auth 도메인에 있지만, pet 서비스가 이미 `LoadUserPort`·`UserCode`를 통해 auth에 의존한다. 이번에는 패키지 이동 없이 기존 `UserId`를 사용하고, 여러 도메인에서 같은 타입이 확산되면 shared kernel 이동을 별도 설계 과제로 검토한다.

### 사용자 승인 기록

- 2026-09-09: 사용자가 앞선 JWT/principal 전환 작업을 취소하고, `requireUserId`의 `Long → UserId` 반환 전환만 진행하도록 지시했다.
- 2026-09-09: 사용자가 출생 연도 허용 범위를 최근 30년 이내로 확정하고 구현을 지시했다.
- 2026-09-09: 검토에서 helper 반환 타입만 바꾸고 즉시 `.value`를 쓰는 중간 상태는 유지하지 않으며, pet 식별자 경계를 `UserId`로 일관되게 전환하는 방향을 권고했다. 사용자가 작업 문서 선갱신을 지시했다.
- 2026-09-09: `requireUserId` 중복이 pet 6곳 말고 auth `UserAgreementService`에도 동일하게 있다는 걸 확인 — 사용자가 "다음 단계로 고려할 만한 것들 전부 진행" 지시에 이 정리를 포함시켰다. 공유 컴포넌트는 `LoadUserPort`/`UserCode`/`UserId`/`AuthErrorCode`를 이미 소유한 `auth.application.service`에 두기로 했다 — `PetLockOperations`가 인프라에 안 닿는 포트 조합 헬퍼를 포트 아닌 평범한 `@Component`로 둔 것과 같은 근거(KD3-497)를 그대로 따른다. `operator fun invoke`로 만들어 호출부 문법(`requireUserId(command.userCode)`)이 기존과 동일하게 유지되도록 했다.
- 2026-09-09: 독립 리뷰(fresh subagent)가 `DeletePetServiceTest`/`GetPetServiceTest`/`SetRepresentativeServiceTest`/`UpdatePetServiceTest` 4곳에 "사용자 없음(NOT_FOUND_USER)" 케이스 테스트가 없다는 걸 발견 — merge-base(`epic/KD3-404-pet-domain-migration` 최신 tip) 기준으로 이 브랜치 이전부터 있던 갭이라 이번 회귀는 아님을 확인했다. 필수는 아니라고 판단했으나, `RequireUserId` 배선을 이미 4곳 다 건드려놔서 보강 비용이 낮아 사용자가 지금 같이 채우도록 지시했다.

## 완료 확인 기준

- 6개 pet 서비스의 `requireUserId`가 `UserId`를 반환한다.
- `Pet.userId`와 pet application/port의 사용자 식별자 파라미터가 `UserId`다. 인증 경계의 `UserCode` command는 제외한다.
- persistence adapter 밖에서는 `UserId.value`를 사용하지 않는다.
- JPA repository/entity는 기존 DB 스키마와 동일한 `Long` FK로 동작한다.
- 전체 빌드가 통과한다.
- 출생 연도의 최저·최고 허용값, 범위 초과, 미래값, null을 도메인 테스트로 검증한다.
- `requireUserId` 중복 7곳(pet 6개, auth 1개)이 전부 `RequireUserId` 공유 컴포넌트를 쓴다.

### 검증 결과

**검증 가능성에 대한 안내**: 아래 항목 중 CI 링크가 있는 것(`./gradlew build`)은 PR 페이지에서 누구나 재확인할 수 있다. 나머지(로컬 개별 테스트 실행, 로컬 HTTP e2e, 독립 리뷰)는 이 세션이 로컬에서 직접 수행하고 그 결과를 여기 서술로 기록한 것으로, 로그·산출물이 저장소나 PR diff에 남지 않는다 — PR diff만으로는 확인 불가하다(코드 결함으로 단정할 근거도 아니고, 검증이 안 됐다는 뜻도 아니다. KD3-497 PR의 동일 지적에 대응한 것과 같은 방식). 재확인이 필요하면 각 항목에 적힌 커맨드로 직접 재실행할 수 있다.

- `./gradlew build`(ktlint, ArchUnit, 전체 테스트) 통과(최종 커밋 `01023b3` 기준) — CI(`build`) 체크로 재확인 가능: https://github.com/PetCampus-Inc/daeng_v2_back/actions/runs/34336325557
- (로컬 실행, PR diff만으로는 확인 불가) `./gradlew test --tests "*.pet.domain.PetTest"` — 생성 시 최저·최고 허용값, 범위 초과, 미래값, null과 수정 시 범위 초과를 검증했다.
- (로컬 실행, PR diff만으로는 확인 불가) `./gradlew test --tests "*.pet.*"` — pet 도메인·서비스·영속성·동시성 테스트의 UserId 전환 회귀가 없음을 확인했다.
- **독립 리뷰(fresh subagent, 2026-09-09, PR diff만으로는 확인 불가 — 서브에이전트 실행 결과라 로그가 저장소에 안 남음)**: merge-base(`365134e`, epic 최신 tip)부터 전체 diff를 처음부터 읽고 `./gradlew clean build`를 직접 재실행해 검증(43초, 전체 통과). `UserId`↔`Long` 이중 wrap·언랩 누락 여부를 레포 전체 grep으로 확인(없음), `RequireUserId` 7개 호출부가 기존과 동일한 에러코드·예외로 동작하는지 확인, `validateBirthYear` 경계값(양끝 inclusive)·`reconstitute` 제외 일관성 확인, `HexagonalArchitectureTest`를 직접 실행해 ArchUnit 위반 없음을 확인(2초 통과), 안 쓰는 import·죽은 코드·신규 주석 없음을 확인. **로직 결함 발견 없음.** 유일한 발견 사항(테스트 커버리지 갭 4곳)은 위 "확정 사항"에 기록하고 즉시 반영함.
- **로컬 MySQL 실제 HTTP e2e(2026-09-09, 로컬 실행·재현 가능하나 저장된 로그·산출물 없음)**: birthYear 검증만 실제 요청으로 확인(UserId 타입 전환은 API 계약에 영향 없어 e2e 대상 아님). 로컬 서버(`--spring.profiles.active=local`)에 테스트 사용자(`E2E500AA`)를 추가해 검증(검증 후 데이터 삭제):
  - 등록 시 `birthYear = 현재연도-30`(최저 허용) → 201 확인
  - 등록 시 `birthYear = 현재연도`(최고 허용) → 201 확인
  - 등록 시 `birthYear = 현재연도-31`(범위 초과) → 400 `INVALID_INPUT_VALUE` 확인
  - 등록 시 `birthYear = 현재연도+1`(미래) → 400 `INVALID_INPUT_VALUE` 확인
  - 등록 시 `birthYear` 생략 → 201(null 허용) 확인
  - PATCH로 범위 초과 `birthYear` 수정 시도 → 400 확인(update 경로도 동일하게 검증됨)
- **NOT_FOUND_USER 테스트 보강(2026-09-09)**: `DeletePetServiceTest`/`GetPetServiceTest`/`SetRepresentativeServiceTest`/`UpdatePetServiceTest`의 `FakeLoadUserPort`를 `Long?`(nullable)로 바꿔 "사용자 없음" 시나리오를 표현할 수 있게 하고, 각각 `존재하지 않는 사용자면 NOT_FOUND_USER를 던진다` 테스트를 추가(`CreatePetServiceTest`/`GetPetsServiceTest`/`UserAgreementServiceTest`는 이미 있었음 — 이제 7곳 전부 커버). 이 항목은 실제 테스트 코드가 diff에 포함돼 있어 PR diff에서 직접 확인 가능하다 — `./gradlew build` CI 통과(위 링크)가 이 테스트들의 실행 근거다.

## 작업 후 확인 목록

| 대상 | 판정 | 근거 |
|---|---|---|
| `docs/work/KD3-500-user-id-value-object-boundary.md` | 갱신 | 범위·결정·검증 결과 기록 |
| `docs/domains/pet.md` | 갱신 | 출생 연도 허용 범위와 UserId 경계 변경을 장기 도메인 제약으로 기록(사용자 식별자 행 추가) |
| `docs/inventory/api.md` | 확인했지만 변경 없음 | 공개 API 계약 변경 없음 |
| `auth/application/service/RequireUserId.kt` | 신규 | `requireUserId` 중복 7곳을 대체하는 공유 컴포넌트 |
| `auth/application/service/UserAgreementService.kt` | 갱신 | 자체 `requireUserId` 제거, `RequireUserId` 주입으로 전환 |
| pet 서비스 6개 | 갱신 | 자체 `requireUserId` 제거, `RequireUserId` 주입으로 전환 |
| 관련 테스트 7개 | 갱신 | `RequireUserId(Fake/StubLoadUserPort(...))`로 실제 컴포넌트에 Fake 포트를 감싸 주입 |
