> 생성: 2026-09-02 19:24 · 최종 수정: 2026-09-08 11:40

# KD3-433 pet 대표견 설정 API 구축

| 항목 | 값 |
|---|---|
| Jira | `KD3-433` |
| 브랜치 | `feat/KD3-433-pet-representative-api` (2026-09-07 생성, 사용자 지시로 AI가 생성 — 통상 절차와 달리 사람이 Jira에서 미리 만들어 제공하지 않음) |
| 상위 에픽 | `KD3-404` |

## 현재 제어점

- 활성 workflow: `003-migration`
- 현재 공통 단계: `5`
- 다음 결정 또는 전환 조건: 구현·테스트·문서·독립 리뷰·로컬 e2e 검증 전부 완료. [PR #21](https://github.com/PetCampus-Inc/daeng_v2_back/pull/21) 생성(base `feat/KD3-431-pet-profile-create-update-api` — 이유는 "확정 사항" 참고). PR 생성 후 CTO 관점 락 설계 재검토(2026-09-08) — `UpdatePetService` lost-update를 KD3-433 안에서 비관적 락으로 임시로 고쳐봤다가, 필요 이상으로 무겁다고 판단해 되돌리고 `@Version`(낙관적 락)으로 KD3-431에서 처리하기로 결정. **KD3-431에 `@Version` 반영·독립 리뷰·커밋·푸시까지 완료됐고, 이 브랜치를 그 위로 rebase·force-push 완료했다(아래 "알려진 리스크" 1번 참고)** — 이제 이 브랜치도 낙관적 락 보호를 받는다. 다음: PR #21 리뷰 대응, KD3-431이 epic에 머지되면 base 재조정·rebase.

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
- 전체 빌드: `./gradlew build --rerun-tasks` 통과(ktlint, ArchUnit 포함). 최초 구현 시점엔 149건 통과 확인(`build/test-results/test/*.xml` 합산, 로컬 실행). KD3-431의 `@Version` rebase 반영 후 로컬에서 재실행해 151건, 실패·에러 0건 확인(마찬가지로 `build/test-results/test/*.xml` 합산 — 이 실행 자체의 로그·artifact는 PR diff에 첨부돼 있지 않다). 같은 커밋(`d473f01`) 기준 GitHub Actions CI의 `build` 체크도 별도로 통과했다 — [CI 실행 로그](https://github.com/PetCampus-Inc/daeng_v2_back/actions/runs/34180174440/job/101917498524)에서 초록불(BUILD SUCCESSFUL)을 직접 확인할 수 있다(정확한 테스트 건수까지는 CI 로그 리포터 출력 형식상 한 줄로 안 잡혀서, "151건"이라는 정확한 숫자는 로컬 실행 결과가 근거이고 CI는 그 실행이 실패 없이 통과했다는 것의 독립적 재확인이다).
- 문서: `docs/domains/pet.md`에 "pet 대표견 설정 API" 절 추가(엔드포인트, 대표견 전환 처리, 동시성 처리, 소유권·상태 검증, 응답 형식). `docs/inventory/api.md`의 `POST /api/v0/pet/representative/{petId}`를 `KEEP`에서 `REDESIGN`으로 정정(KD3-431/432와 동일한 형식으로 `PUT /api/v1/pets/{petId}/representative` 재설계 확정 근거 기록).

## 독립 리뷰 결과

fresh subagent 리뷰(읽기 전용, 실제 코드·빌드 결과 확인 지시) 수행. 발견 사항 3건, 모두 반영:

1. **lost-update 버그(실사용 영향 가능성 있음, 수정 완료)**: `SetRepresentativeService`가 잠금 없이 `loadPetPort.findById`로 읽은 `pet` 객체를, `PetPersistenceAdapter.setRepresentativeWithinLock`이 락 안에서 그대로 `save()`했다. `save()`는 `Pet`의 모든 필드로 `PetJpaEntity`를 새로 만들어 저장하는 전체 행 덮어쓰기라, 이 pet에 대해 대표견 설정 요청과 동시에(예: `PATCH /api/v1/pets/{petId}`) 다른 필드 수정이 커밋되면 그 수정이 조용히 되돌아갈 수 있었다(`UpdatePetService`는 이 pet 행에 대한 별도 잠금이 없어 이 레이스를 막지 못한다). 수정: `setRepresentativeWithinLock`이 대상 pet도 잠금 보호된 `findAllActiveByUserIdForUpdate` 조회 결과에서 다시 찾아(`activePets.find { it.id == pet.id }`) 그 최신 상태로만 멱등성 검사·수정·저장하도록 바꿨다 — 매개변수로 받은 `pet`은 이제 `userId`·`id`를 식별하는 용도로만 쓰이고 절대 그대로 저장되지 않는다. 재발 방지 테스트(`PetPersistenceAdapterTest`의 "setRepresentativeWithinLock 호출 전에 다른 필드가 동시에 변경돼도 그 변경을 덮어쓰지 않는다")를 추가했고, 수정 전 코드로 되돌려 이 테스트가 실제로 실패하는 것과 수정 후 통과하는 것을 직접 확인했다.
2. **문서 오기**: `docs/domains/pet.md`에 레거시 대표견 설정 엔드포인트를 `PUT /api/v0/pet/representative/{petId}`로 적었으나, 실제 레거시(`daeng_v1_back`의 `PetController.java`)는 `POST`다. 같은 diff의 `docs/inventory/api.md`에는 이미 `POST`로 정확히 적혀 있어 문서 간 모순이었다. `POST`로 정정했다.
3. **테스트 커버리지 공백**: 위 1번 버그를 직접 검증하는 테스트가 없었다는 지적 — 재발 방지 테스트 추가로 해소.

반대 방향(다른 요청이 대표견 플래그 변경을 덮어쓰는 경우)은 이 1차 독립 리뷰 시점엔 `UpdatePetService`가 애초에 이 pet 행에 대한 잠금이 없는 이 프로젝트의 기존 구조적 한계이고 KD3-433이 새로 만든 문제가 아니라며 이번 수정 범위에서 제외했다. **이후 상태 갱신(2026-09-08): 이 갭은 더 이상 미해결이 아니다** — KD3-431에 도입한 `@Version`(낙관적 락)으로 해결 완료됐다. 경위와 근거는 아래 "알려진 리스크" 1번 참고.

### 2차 리뷰 (자동화 리뷰 도구, 2026-09-08, PR #21)

4. **서비스 계층에 `@Transactional` 누락(실사용 영향 가능성 있음, 수정 완료)**: `SetRepresentativeService.setRepresentative()`에 `@Transactional`이 없었다(`CreatePetService`·`UpdatePetService`는 있음 — 이 서비스만 빠진 불일치였다). Spring 기본 전파(`REQUIRED`) 규칙상, 호출부에 활성 트랜잭션이 없으면 `savePetPort.setRepresentativeWithinLock(pet)`(자체 `@Transactional`) 호출이 독립적인 트랜잭션 하나로 시작·커밋된다. 그 뒤에 실행되는 `checkNotNull(loadBreedPort.findById(updated.breedId))`가 breed 부재로 예외를 던지면, 대표견 변경은 이미 커밋된 채로 API는 500을 반환한다 — 롤백되지 않는 부분 커밋. 수정: `setRepresentative()`에 `@Transactional`을 추가해 대표견 저장과 breed 조회를 하나의 트랜잭션으로 묶었다. 검증: `SetRepresentativeTransactionBoundaryTest`(Testcontainers 실제 MySQL)를 새로 추가 — `PetJpaEntity.breed`의 `@JoinColumn`이 `ConstraintMode.NO_CONSTRAINT`라 실제 FK가 없다는 점을 이용해 breed 행을 미리 지워 breed 조회 실패를 재현하고, 대표견 변경이 DB에 전혀 반영되지 않았음을 직접 확인한다. 수정 전 코드로 되돌려 이 테스트가 실패하는 것도 확인했다 — 다만 예상했던 "롤백 안 됨" 단언 실패가 아니라 `UserJpaEntity.addresses` 지연 컬렉션 접근 시점의 `LazyInitializationException`이 먼저 터졌다(활성 Hibernate 세션 자체가 없어서 발생 — `@Transactional` 부재라는 같은 근본 원인의 더 이른 증상). 수정 적용 후 재실행하면 정상 통과.

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
- **KD3-431 rebase(`@Version` 반영) 후 재검증(2026-09-08)**: `setRepresentativeWithinLock`이 이제 `@Version`이 붙은 pet을 다루게 됐는데, 비관적 락(`LockUserPort`) 기반 흐름과 낙관적 락이 서로 나쁘게 상호작용해 스퓨리어스 409가 나지 않는지 실제로 확인했다. user 1명에게 pet 5마리(최대치)를 만들고 **5마리 전부에 동시에 대표견 설정 요청**을 보냈다 — 전부 200(409 없음), 최종적으로 정확히 1마리만 대표견, 관련된 모든 pet의 `version`이 각자 정확히 1씩 증가함을 DB로 직접 확인. 그 외 정상 설정(버전 0→1 증가 확인)·반복 요청(멱등, 버전 불변 확인)·타인 pet 거부(403)·존재하지 않는 pet(404)·삭제된 pet 거부(404)도 재확인.

## 알려진 리스크 (후속 처리 필요)

검토(2026-09-08)에서 나온 지적. "나중에 고려해볼 개선사항"이 아니라 지금도 재현 가능한 문제라 명시적으로 남긴다.

1. **~~`UpdatePetService`가 `setRepresentativeWithinLock`의 락 체계 밖에 있어 lost-update 가능~~ — 해결 완료(2026-09-08)**: `PATCH /api/v1/pets/{petId}`(필드 수정)는 원래 `LockUserPort.lockById`를 전혀 호출하지 않았다. 같은 pet에 대해 PATCH와 `PUT .../representative`가 거의 동시에 들어오면, 먼저 커밋된 대표견 상태를 뒤이은 PATCH가 자신이 읽어둔 낡은 상태로 덮어써 조용히 되돌릴 수 있었다 — `setRepresentativeWithinLock` 안에서 고친 lost-update 버그(위 "독립 리뷰 결과" 1번)와 같은 패턴이 반대쪽(`UpdatePetService`)에 남아 있던 것. **경위**: `setRepresentativeWithinLock`과 같은 방식(`users` 행 잠그고 활성 pet 전체 재조회)으로 KD3-433 안에서 임시로 고쳐봤으나(`SavePetPort.updateWithinLock`), CTO 관점 재검토 결과 pet 하나만 건드리면 되는 `UpdatePetService`엔 필요 이상으로 무거운 방식이라 판단해 KD3-433에서는 되돌렸다(서로 무관한 pet A·B 수정까지 같은 락으로 불필요하게 직렬화됨). **최종 해법**: `PetJpaEntity`에 `@Version`(낙관적 락)을 도입 — `registerWithinLimit`/`setRepresentativeWithinLock`은 "여러 행에 걸친 불변식(최대 5마리, 대표견 유일성)"을 지켜야 해서 낙관적 락만으로는 부족해 지금의 `users` 행 비관적 락을 그대로 유지하고, `UpdatePetService`만 `@Version` + 일반 `save()`로 전환했다(`OptimisticLockingFailureException`을 `GlobalExceptionHandler`에서 409 `RESOURCE_CONFLICT`로 매핑). `PetJpaEntity`·`UpdatePetService`가 원래 KD3-431 소유 파일이라 KD3-431 PR(#17)에서 구현·독립 리뷰·커밋·푸시까지 완료했고(실제 동시 요청 8건으로 e2e 검증: 1건 성공·7건 409), 이 브랜치(KD3-433)를 그 위로 rebase·force-push해 반영했다 — 이제 이 브랜치도 낙관적 락 보호를 받는다. 실무 근거: Baeldung·Vlad Mihalcea(Hibernate 코어) 모두 낙관적 락을 기본 선택으로 권장, RFC 7231이 409를 버전 충돌의 정확한 용도로 명시.
2. **락 대기 타임아웃 처리 없음(운영 리스크, 우선순위 낮음, 미해결)**: `SELECT ... FOR UPDATE`(`LockUserPort.lockById`, `findAllActiveByUserIdForUpdate`)가 InnoDB 기본 `innodb_lock_wait_timeout`(50초)에 걸렸을 때 의미 있는 클라이언트 응답(409 등)이 없다 — 지금은 `GlobalExceptionHandler`의 catch-all(500)로 떨어진다.
3. **`users` 행이 여러 기능의 공용 락 지점이 되고 있음(운영 리스크, 우선순위 낮음, 미해결)**: 지금은 pet 등록·대표견 설정 두 기능만 이 락을 쓰지만, 앞으로 같은 패턴("항상 존재하는 부모 행 잠그기")을 다른 기능에도 계속 쓰면 서로 무관한 기능들이 같은 물리적 락 하나를 놓고 경쟁하게 된다.

**처리 방침**: 1번은 처리 완료(KD3-431에서 구현, KD3-433은 rebase로 반영받음). 2·3번은 지금 트래픽 규모에서는 급하지 않아 별도 티켓으로 다루기로 했다(Jira 티켓은 사용자 지시 전까지 생성하지 않음).

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
| `SetRepresentativeService.kt` | 갱신 | `@Transactional` 누락 수정(2차 리뷰 4번) |
| `SetRepresentativeTransactionBoundaryTest.kt` | 신규 | 위 수정의 재발 방지 테스트(Testcontainers) |
