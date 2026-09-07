> 생성: 2026-09-02 19:24 · 최종 수정: 2026-09-07 23:55

# KD3-433 pet 대표견 설정 API 구축

| 항목 | 값 |
|---|---|
| Jira | `KD3-433` |
| 브랜치 | `feat/KD3-433-pet-representative-api` (2026-09-07 생성, 사용자 지시로 AI가 생성 — 통상 절차와 달리 사람이 Jira에서 미리 만들어 제공하지 않음) |
| 상위 에픽 | `KD3-404` |

## 현재 제어점

- 활성 workflow: `003-migration`
- 현재 공통 단계: `4`
- 다음 결정 또는 전환 조건: 구현·단위 테스트·영속성 어댑터 테스트·동시성 테스트(Testcontainers)까지 작성 완료. 독립 리뷰(fresh subagent) 1건 수행, lost-update 버그 1건 발견해 수정. 로컬 HTTP e2e 검증에서 **독립 리뷰도 자동화 테스트도 못 잡은 두 번째 실제 버그**(같은 트랜잭션 안 update 두 개의 flush 순서가 뒤바뀌어 UNIQUE 제약 위반 500)를 발견해 수정했다(아래 "독립 리뷰 결과", "로컬 HTTP e2e 검증 결과" 참고). 동시성 테스트도 개별 요청 실패를 마스킹하고 있던 걸 발견해 `Future.get()`으로 실제로 실패를 감지하도록 고쳤다. `./gradlew build --rerun-tasks` 전체 149건 통과 확인(ktlint·ArchUnit·전 테스트 포함). API 인벤토리·pet 도메인 문서 갱신 완료. 사용자가 URL 설계와 검증 계획을 사후 검토해 승인했다(아래 "사용자 승인 기록" 참고). 다음: 커밋 → 푸시 → PR 생성(base `feat/KD3-431-pet-profile-create-update-api`) 순서로 진행. 작업 브랜치는 `dev`/`epic`이 아니라 `feat/KD3-431-pet-profile-create-update-api`에서 분기한 stacked 브랜치다 — `PetErrorCode`/`LoadPetPort`/`SavePetPort`/`PetResponse`가 KD3-431에서 만들어졌고 `epic/KD3-404-pet-domain-migration`엔 아직 없기 때문이다. **KD3-432 위에는 쌓지 않는다** — 433이 실제로 쓰는 건 431의 산출물뿐이고 432(조회 API)는 전혀 재사용하지 않아, 432 위에 쌓으면 432가 나중에 바뀔 때마다 433도 불필요하게 리베이스해야 하는 문제만 생긴다(432→431 리베이스 필요 상황을 직접 겪고 확인).

## 작업 목표

인증된 사용자가 자신의 pet 하나를 대표견으로 멱등하게 설정할 수 있는 v1 API를 제공한다.

## 작업 범위

- `PUT /api/v1/pets/{petId}/representative`를 구현한다.
- 대상 pet의 소유권·활성 상태를 검증한다.
- 같은 사용자에게 대표견이 최대 하나만 존재하도록 보장한다.
- 반복 요청과 동시 요청의 결과를 검증한다.

## 작업 제외 범위

- pet 프로필 생성·수정·삭제
- 유치원 대표 pet 또는 유치원 연결 상태
- 레거시 v0 대표 설정 API 호환

## 방향 논의 및 결정 사항

### 확정 사항

- 대표 설정은 상태를 설정하는 멱등 연산이므로 POST가 아니라 PUT을 사용한다.
- 대표견 단일성은 KD3-430의 저장·동시성 정책을 따른다.
- 작업 브랜치는 `dev`가 아닌 `feat/KD3-431-pet-profile-create-update-api`에서 분기한 stacked 브랜치로 진행한다. 이 티켓의 API가 KD3-431의 `PetErrorCode`(소유권·미존재 에러)·`LoadPetPort`/`SavePetPort`(조회·저장)·`PetResponse`(응답 DTO)에 기능적으로 의존하기 때문이다. KD3-432(조회 API)는 재사용하지 않아 그 위에는 쌓지 않는다. PR base는 KD3-431이 `epic/KD3-404-pet-domain-migration`에 머지되기 전까지 `feat/KD3-431-pet-profile-create-update-api`로 두고, 머지 후 재조정한다.

### 미결 질문

- 없음.

### 사용자 승인 기록

- 2026-09-02: 사용자가 대표견 설정을 독립 유스케이스로 분리하고 RESTful v1 API를 승인했다.
- 2026-09-07: 구현 완료 후 사용자가 URL 설계(`PUT /api/v1/pets/{petId}/representative`)를 직접 검토했다. `PUT /api/v1/pets/{petId}`(기존 pet 리소스 URI 재사용) 대안과 비교해 논의한 뒤, 이 리소스가 pet 전체가 아니라 "이 petId가 대표견 슬롯을 차지하는가"라는 독립된 이진 상태이고 이미 `PATCH /pets/{petId}`가 부분 필드 수정 의미를 선점하고 있어 같은 URI에 PUT을 얹으면 의미가 충돌한다는 근거로 현재 방식(`/{petId}/representative` 서브리소스, body 없는 PUT)을 승인했다. 컬렉션 레벨(`PUT /pets/representative` + body) 대안은 검토했으나 채택하지 않았다. 동시성·검증 계획(단위/영속성 어댑터/Testcontainers 동시성 테스트, 독립 리뷰에서 발견한 lost-update 버그 수정)도 함께 승인했다.

## 구현 및 검증 결과

- 구현: `SetRepresentativeUseCase`/`SetRepresentativeCommand`/`SetRepresentativeResult`(input port), `SetRepresentativeService`(소유권·상태 검증 후 `SavePetPort.setRepresentativeWithinLock` 위임, breed 조회는 KD3-432와 동일하게 `checkNotNull` 사용 — 저장된 pet이 참조하는 breed 누락은 클라이언트 책임이 아니라 서버 데이터 정합성 문제이므로 400이 아닌 500), `SetRepresentativeController`(`PUT /api/v1/pets/{petId}/representative`, `PetResponse` 반환).
- `SavePetPort.setRepresentativeWithinLock` 구현(`PetPersistenceAdapter`): 이미 대표견이면 DB 쓰기 없이 그대로 반환(멱등). 아니면 `LockUserPort.lockById`로 `users` 행을 먼저 잠그고, `registerWithinLimit`이 쓰던 `findAllActiveByUserIdForUpdate` 잠금 쿼리를 재사용해 기존 대표견을 해제한 뒤 대상 pet을 대표견으로 지정한다 — 신규 조회 쿼리를 추가하지 않고 기존 최대 마릿수 등록의 동시성 패턴을 그대로 재사용했다.
- 단위 테스트: `SetRepresentativeServiceTest`(정상 설정/이미 대표견인 경우 멱등/미존재 pet 404/삭제된 pet 404/타인 pet 403, 5건) 전부 통과.
- 영속성 어댑터 테스트: `PetPersistenceAdapterTest`에 `setRepresentativeWithinLock` 케이스 3건(이미 대표견이면 유지, 기존 대표견 해제 후 대상 지정, 대표견이 없는 상태에서도 지정 가능) 추가, 전부 통과(기존 6건 포함 9건).
- 동시성 테스트: `PetSetRepresentativeConcurrencyTest`(신규, `PetRegistrationConcurrencyTest`와 동일하게 Testcontainers MySQL 사용) — 동일 사용자의 pet 5마리에 대해 대표견 설정 요청 5건을 동시에 실행해도 최종적으로 대표견이 1건만 남음을 확인.
- 전체 빌드: `./gradlew build --rerun-tasks` 통과(ktlint, ArchUnit 포함). 전체 테스트 149건 통과, 실패·에러 0건(`build/test-results/test/*.xml` 합산으로 직접 확인).
- 문서: `docs/domains/pet.md`에 "pet 대표견 설정 API" 절 추가(엔드포인트, 대표견 전환 처리, 동시성 처리, 소유권·상태 검증, 응답 형식). `docs/inventory/api.md`의 `POST /api/v0/pet/representative/{petId}`를 `KEEP`에서 `REDESIGN`으로 정정(KD3-431/432와 동일한 형식으로 `PUT /api/v1/pets/{petId}/representative` 재설계 확정 근거 기록).

## 독립 리뷰 결과

fresh subagent 리뷰(읽기 전용, 실제 코드·빌드 결과 확인 지시) 수행. 발견 사항 3건, 모두 반영:

1. **lost-update 버그(실사용 영향 가능성 있음, 수정 완료)**: `SetRepresentativeService`가 잠금 없이 `loadPetPort.findById`로 읽은 `pet` 객체를, `PetPersistenceAdapter.setRepresentativeWithinLock`이 락 안에서 그대로 `save()`했다. `save()`는 `Pet`의 모든 필드로 `PetJpaEntity`를 새로 만들어 저장하는 전체 행 덮어쓰기라, 이 pet에 대해 대표견 설정 요청과 동시에(예: `PATCH /api/v1/pets/{petId}`) 다른 필드 수정이 커밋되면 그 수정이 조용히 되돌아갈 수 있었다(`UpdatePetService`는 이 pet 행에 대한 별도 잠금이 없어 이 레이스를 막지 못한다). 수정: `setRepresentativeWithinLock`이 대상 pet도 잠금 보호된 `findAllActiveByUserIdForUpdate` 조회 결과에서 다시 찾아(`activePets.find { it.id == pet.id }`) 그 최신 상태로만 멱등성 검사·수정·저장하도록 바꿨다 — 매개변수로 받은 `pet`은 이제 `userId`·`id`를 식별하는 용도로만 쓰이고 절대 그대로 저장되지 않는다. 재발 방지 테스트(`PetPersistenceAdapterTest`의 "setRepresentativeWithinLock 호출 전에 다른 필드가 동시에 변경돼도 그 변경을 덮어쓰지 않는다")를 추가했고, 수정 전 코드로 되돌려 이 테스트가 실제로 실패하는 것과 수정 후 통과하는 것을 직접 확인했다.
2. **문서 오기**: `docs/domains/pet.md`에 레거시 대표견 설정 엔드포인트를 `PUT /api/v0/pet/representative/{petId}`로 적었으나, 실제 레거시(`daeng_v1_back`의 `PetController.java`)는 `POST`다. 같은 diff의 `docs/inventory/api.md`에는 이미 `POST`로 정확히 적혀 있어 문서 간 모순이었다. `POST`로 정정했다.
3. **테스트 커버리지 공백**: 위 1번 버그를 직접 검증하는 테스트가 없었다는 지적 — 재발 방지 테스트 추가로 해소.

반대 방향(다른 요청이 대표견 플래그 변경을 덮어쓰는 경우)은 `UpdatePetService`가 애초에 이 pet 행에 대한 잠금이 없는 이 프로젝트의 기존 구조적 한계이고 KD3-433이 새로 만든 문제가 아니라 이번 수정 범위에 포함하지 않았다 — 필요하면 별도 티켓으로 다룬다.

## 로컬 HTTP e2e 검증 결과

로컬 MySQL(Docker)·`./gradlew bootRun --args='--spring.profiles.active=local'`로 실제 서버를 띄우고, HS256 JWT를 직접 서명해 실제 HTTP 요청으로 검증했다(테스트 사용자·pet은 검증 후 DB에서 직접 삭제).

- **정상 설정 (1차 시도, 500 발견)**: 사용자 1명에게 pet 2마리(첫 등록이 자동 대표견)를 만들고, 두 번째 pet에 `PUT /api/v1/pets/{petId}/representative`를 보냈더니 `500 INTERNAL_SERVER_ERROR`가 났다. 서버 로그: `Duplicate entry '3' for key 'pets.uk_pets_representative_user'`. 이 케이스는 단위 테스트도, `PetPersistenceAdapterTest`(H2 `@DataJpaTest`)도, `PetSetRepresentativeConcurrencyTest`(Testcontainers 실제 MySQL)도 잡지 못했었다.
  - **원인**: `setRepresentativeWithinLock`이 "기존 대표견 해제 저장" → "대상 pet 대표견 지정 저장" 순서로 `save()`(내부적으로 `EntityManager.merge()`)를 두 번 호출하지만, 둘 다 즉시 flush되지 않고 트랜잭션 커밋 시점까지 미뤄진다. Hibernate가 호출 순서와 다르게 SQL을 내보내면서 "대표견 지정"이 "기존 해제"보다 먼저 실행됐고, 그 순간 두 pet 행이 동시에 같은 `representative_user_id`를 가져 MySQL의 즉시 검사(immediate) UNIQUE 제약을 위반했다.
  - **수정**: 두 저장 사이에 `entityManager.flush()`를 명시적으로 호출해(`flushClearedRepresentativesBeforeReassigning()`) 호출 순서와 SQL 실행 순서를 일치시켰다. 수정 전/후를 실제로 curl로 재현·재검증해 500 → 200으로 바뀌는 것을 직접 확인했다.
  - **왜 기존 테스트가 못 잡았는지**: H2(`@DataJpaTest`)는 이 케이스에서 이유를 특정하진 못했지만 문제를 재현하지 않았다(동일 코드·동일 시나리오가 계속 통과). Testcontainers 동시성 테스트도 처음엔 놓쳤는데, 원인이 버그와 무관하게 테스트 자체의 결함이었다 — `executor.submit { ... }`의 반환값(`Future`)을 버리고 있어서 스레드 안에서 던진 예외가 조용히 사라지고 최종 집계(대표견 1건)만 확인했다. `futures.map { it.get(...) }`로 각 요청의 실패를 실제로 검사하도록 고쳤고, 고친 뒤 버그를 되살려 재현해보니 이 테스트도 이제 확실히 실패하는 것을 확인했다.
- **반복 요청(멱등)**: 같은 pet에 다시 `PUT`을 보내도 200, 상태 변화 없이 재확인.
- **타인 pet 거부**: 다른 사용자의 토큰으로 요청 시 `403 PET-403-1`.
- **존재하지 않는 pet**: `404 PET-404-1`.
- **삭제된 pet 거부**: `deleted_at`을 직접 채운 pet에 요청 시 `404 PET-404-1`(삭제 API는 KD3-434 범위라 DB에서 직접 시뮬레이션).
- **인증 없이 요청**: `401 UNAUTHORIZED_REQUEST`(범위 밖이지만 확인).

## 완료 확인 기준

- [x] 정상 설정, 타인 pet 거부, 삭제된 pet 거부, 반복 요청을 테스트한다. — 단위 테스트 + 로컬 HTTP e2e 둘 다 검증. e2e에서 실제 버그 1건(위 "로컬 HTTP e2e 검증 결과") 발견해 수정.
- [x] 경쟁 요청 뒤에도 대표견이 하나 이하임을 검증한다. — `PetSetRepresentativeConcurrencyTest`로 검증(개별 요청 실패 감지 보강 후).
- [x] API 인벤토리와 pet 도메인 문서 영향을 판정·기록한다.

## 작업 후 확인 목록

| 대상 | 판정 | 근거 |
|---|---|---|
| `docs/work/KD3-433-pet-representative-api.md` | 갱신 | API 결정·검증 결과 기록 |
| `docs/inventory/api.md` | 갱신 | `/api/v0/pet/representative/{petId}`를 `KEEP`→`REDESIGN`으로 정정 |
| `docs/domains/pet.md` | 갱신 | "pet 대표견 설정 API" 절 추가 |
