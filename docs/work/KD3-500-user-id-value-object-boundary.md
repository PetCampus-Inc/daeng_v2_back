> 생성: 2026-09-09 17:30 · 최종 수정: 2026-09-09 18:08

# KD3-500 pet 사용자 식별자 타입 경계 통일

| 항목 | 값 |
|---|---|
| Jira | `KD3-500` |
| 브랜치 | `refactor/KD3-500-jwt-principal-user-id` |
| 상위 에픽 | 해당 없음 |

## 현재 제어점

- 활성 workflow: `003-migration`
- 현재 공통 단계: `4` (구현·검증 완료)
- 다음 결정 또는 전환 조건: 사용자가 커밋을 지시함 — 커밋 후 `requireUserId` 중복 제거 등 후속 정리로 이어감

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

## 작업 제외 범위

- JWT subject, Spring Security principal, access/refresh token, Redis refresh token 구조 변경
- controller·command·포트의 공개 인터페이스 변경
- `UserId`를 auth 패키지 밖 shared kernel로 이동하는 패키지 재구성

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

## 완료 확인 기준

- 6개 pet 서비스의 `requireUserId`가 `UserId`를 반환한다.
- `Pet.userId`와 pet application/port의 사용자 식별자 파라미터가 `UserId`다. 인증 경계의 `UserCode` command는 제외한다.
- persistence adapter 밖에서는 `UserId.value`를 사용하지 않는다.
- JPA repository/entity는 기존 DB 스키마와 동일한 `Long` FK로 동작한다.
- 전체 빌드가 통과한다.
- 출생 연도의 최저·최고 허용값, 범위 초과, 미래값, null을 도메인 테스트로 검증한다.

### 검증 결과

- `./gradlew test --tests "*.pet.domain.PetTest"` 통과(2026-09-09). 생성 시 최저·최고 허용값, 범위 초과, 미래값, null과 수정 시 범위 초과를 검증했다.
- `./gradlew test --tests "*.pet.*"` 통과(2026-09-09). pet 도메인·서비스·영속성·동시성 테스트의 UserId 전환 회귀가 없다.
- `./gradlew ktlintCheck` 통과(2026-09-09).

## 작업 후 확인 목록

| 대상 | 판정 | 근거 |
|---|---|---|
| `docs/work/KD3-500-user-id-value-object-boundary.md` | 갱신 | 범위·결정·검증 결과 기록 |
| `docs/domains/pet.md` | 갱신 | 출생 연도 허용 범위와 UserId 경계 변경을 장기 도메인 제약으로 기록(사용자 식별자 행 추가) |
| `docs/inventory/api.md` | 확인했지만 변경 없음 | 공개 API 계약 변경 없음 |
